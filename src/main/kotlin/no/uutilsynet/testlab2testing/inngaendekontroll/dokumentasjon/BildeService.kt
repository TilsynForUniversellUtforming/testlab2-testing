package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import org.springframework.web.multipart.MultipartFile

private const val GJENNOPPRETT_BILDE_FEIL = "Kunne ikkje gjenopprette bilde"

@Service
class BildeService(
    @Autowired val testResultatDAO: TestResultatDAO,
    val blobClient: BlobStorageClient,
) {

  private val logger = LoggerFactory.getLogger(BildeService::class.java)

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  fun createBilde(testresultatId: Int, bilder: List<MultipartFile>) = runCatching {
    val antallBilder = testResultatDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
    val imageDetails =
        multipartFilesToImageDetails(testresultatId, antallBilder.size, bilder).getOrThrow()

    blobClient.uploadBilder(imageDetails).forEach { bildeResultat ->
      if (bildeResultat.isSuccess) {
        val bildeDetalj = bildeResultat.getOrThrow()
        testResultatDAO
            .saveBilde(testresultatId, bildeDetalj.fullFileName, bildeDetalj.fullThumbnailName)
            .getOrThrow()
      }
    }
  }

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  fun deleteBilder(testresultatId: Int, bildeId: Int? = null) = runCatching {
    val bildeStiList: List<BildeSti> =
        if (bildeId != null) {
          listOf(
              testResultatDAO.getBildeSti(bildeId).getOrThrow()
                  ?: throw IllegalArgumentException("Fann ikkje bilde for bilde-id $bildeId"))
        } else {
          testResultatDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
        }

    bildeStiList.forEach { bildeSti -> deleteBilde(bildeSti) }
  }

  private fun deleteBilde(bildeSti: BildeSti) {
    blobClient.deleteBilde(bildeSti.bilde).onFailure {
      logger.error("Kunne ikkje slette bilde frå blob storage", it)
      throw it
    }

    blobClient.deleteBilde(bildeSti.thumbnail).onFailure {
      blobClient.restoreBilde(bildeSti.bilde).onFailure { ex ->
        logger.error(GJENNOPPRETT_BILDE_FEIL, ex)
      }
      logger.error("Kunne ikkje slette thumbnail frå blob storage")
      throw it
    }

    testResultatDAO.deleteBilde(bildeSti.id).onFailure {
      logger.error("Kunne ikkje slette bilde frå database", it)
      blobClient.restoreBilde(bildeSti.bilde).onFailure { ex ->
        logger.error(GJENNOPPRETT_BILDE_FEIL, ex)
      }
      blobClient.restoreBilde(bildeSti.thumbnail).onFailure { ex ->
        logger.error(GJENNOPPRETT_BILDE_FEIL, ex)
      }

      throw it
    }
  }

  @Cacheable("bildeCache", key = "#testresultatId")
  fun listBildeForTestresultat(testresultatId: Int): Result<List<Bilde>> = runCatching {
    val paths = testResultatDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
    return blobClient.getBildeStiList(paths)
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
      bildeList: List<MultipartFile>
  ): Result<List<BildeRequest>> {
    val allowedMIMETypes =
        listOf(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE, "image/bmp")

    return runCatching {
      bildeList.mapIndexed { index, bilde ->
        val originalFileName =
            bilde.originalFilename ?: throw IllegalArgumentException("Filnamn er tomt")

        require(allowedMIMETypes.contains(bilde.contentType)) {
          "$originalFileName har annen filtype enn ${allowedMIMETypes.joinToString(",")}"
        }

        val fileExtension = originalFileName.substringAfterLast('.').lowercase()

        val image = ImageIO.read(bilde.inputStream)
        val thumbnail = createThumbnailImage(image)

        val bildeIndex = indexOffset + index
        val newFileName = "${testresultatId}_${bildeIndex}"

        BildeRequest(image, thumbnail, newFileName, fileExtension)
      }
    }
  }
}
