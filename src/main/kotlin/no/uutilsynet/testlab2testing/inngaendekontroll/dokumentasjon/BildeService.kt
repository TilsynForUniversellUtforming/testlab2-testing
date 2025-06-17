package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile
import java.awt.Image
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import javax.imageio.ImageIO

private const val GJENNOPPRETT_BILDE_FEIL = "Kunne ikkje gjenopprette bilde"

@Service
class BildeService(
    @Autowired val testResultatDAO: TestResultatDAO,
    @Autowired val bildeDAO: BildeDAO,
    @Lazy val blobClient: ImageStorageService,
) {

  private val logger = LoggerFactory.getLogger(BildeService::class.java)

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  fun createBilde(testresultatId: Int, bilder: List<MultipartFile>) = runCatching {
    val antallBilder = bildeDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
    val kontrolInfo = testResultatDAO.getKontrollForTestresultat(testresultatId).getOrThrow()
    val imageDetails =
        multipartFilesToImageDetails(testresultatId, antallBilder.size, bilder, kontrolInfo)
            .getOrThrow()

    blobClient.uploadBilder(imageDetails).forEach { bildeResultat ->
      if (bildeResultat.isSuccess) {
        val bildeDetalj = bildeResultat.getOrThrow()
        bildeDAO
            .saveBilde(testresultatId, bildeDetalj.fullFileName, bildeDetalj.fullThumbnailName)
            .getOrThrow()
      }
    }
  }

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  fun deleteBilder(testresultatId: Int, bildeId: Int? = null) = runCatching {
    testResultatDAO.getKontrollForTestresultat(testresultatId).getOrThrow()
    val bildeStiList: List<BildeSti> = getBildeSti(bildeId, testresultatId)

    bildeStiList.forEach { bildeSti -> deleteBilde(bildeSti) }
  }

  private fun getBildeSti(bildeId: Int?, testresultatId: Int): List<BildeSti> {
    return if (bildeId != null) {
      getBildeStiFromBildeId(bildeId)
    } else {
      bildeDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
    }
  }

  private fun getBildeStiFromBildeId(bildeId: Int) =
      listOf(
        bildeDAO.getBildeSti(bildeId).getOrThrow()
              ?: throw IllegalArgumentException("Fann ikkje bilde for bilde-id $bildeId"))

  private fun deleteBilde(bildeSti: BildeSti) {
    blobClient.deleteBilde(bildeSti.bilde).onFailure {
      logger.error("Kunne ikkje slette bilde frå blob storage", it)
      throw it
    }

    blobClient.deleteBilde(bildeSti.thumbnail).onFailure {
      restoreBilde(bildeSti.bilde)
      logger.error("Kunne ikkje slette thumbnail frå blob storage")
      throw it
    }

    bildeDAO.deleteBilde(bildeSti.id).onFailure {
      logger.error("Kunne ikkje slette bilde frå database", it)
      restoreBilde(bildeSti.bilde)
      restoreBilde(bildeSti.thumbnail)
      throw it
    }
  }

  private fun restoreBilde(path: String) {
    blobClient.restoreBilde(path).onFailure { ex -> logger.error(GJENNOPPRETT_BILDE_FEIL, ex) }
  }

  @Cacheable("bildeCache", key = "#testresultatId")
  fun listBildeForTestresultat(testresultatId: Int): Result<List<Bilde>> = runCatching {
    testResultatDAO.getKontrollForTestresultat(testresultatId).getOrThrow()
    val paths = bildeDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()

    val bilder = blobClient.getBildeStiList(paths)
    return bilder
  }

  private fun createThumbnailImage(originalImage: BufferedImage): BufferedImage {
    val imageTargetSize = 100
    val resultingImage =
        originalImage.getScaledInstance(imageTargetSize, imageTargetSize, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(imageTargetSize, imageTargetSize, BufferedImage.TYPE_INT_RGB)
    outputImage.graphics.drawImage(resultingImage, 0, 0, null)

    return outputImage
  }

  private fun multipartFilesToImageDetails(
      testresultatId: Int,
      indexOffset: Int,
      bildeList: List<MultipartFile>,
      kontrolInfo: KontrollDocumentation
  ): Result<List<BildeRequest>> {
    val allowedMIMETypes =
        listOf(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE, "image/bmp")

    return runCatching {
      bildeList.mapIndexed { index, bilde ->
        val originalFileName =
            bilde.originalFilename ?: throw IllegalArgumentException("Filnamn er tomt")

        logger.info("Content type: ${bilde.contentType}")

        require(allowedMIMETypes.contains(bilde.contentType)) {
          "$originalFileName har annen filtype enn ${allowedMIMETypes.joinToString(",")}"
        }

        val fileExtension = originalFileName.substringAfterLast('.').lowercase()

        val image = ImageIO.read(bilde.inputStream)
        val thumbnail = createThumbnailImage(image)

        val bildeIndex = indexOffset + index
        val directory = getDirectory(kontrolInfo)
        val newFileName = "${directory}${testresultatId}_${bildeIndex}"

        BildeRequest(image, thumbnail, newFileName, fileExtension)
      }
    }
  }

  private fun getDirectory(kontrolInfo: KontrollDocumentation) =
      "${kontrolInfo.kontrollId}${kontrolInfo.tittel}/"

  fun getBilde(bildesti: String): HttpURLConnection {
    return blobClient.getBildeSti(bildesti)
  }

  fun erBildePublisert(bildeSti: String) {
    bildeDAO.erBildeTilPublisertTestgrunnlag(bildeSti)

  }
}
