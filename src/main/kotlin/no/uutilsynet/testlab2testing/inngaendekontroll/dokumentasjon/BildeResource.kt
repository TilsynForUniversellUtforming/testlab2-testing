package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.awt.Image
import java.awt.image.BufferedImage
import java.time.Instant
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.CloudImageDetails
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/bilder")
class BildeResource(
    val testResultatDAO: TestResultatDAO,
    val blobClient: BlobStorageClient,
) {
  val logger: Logger = LoggerFactory.getLogger(BildeResource::class.java)

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  @PostMapping("/{testresultatId}")
  fun createImages(
      @PathVariable testresultatId: Int,
      @RequestParam("bilder") bilder: List<MultipartFile>
  ): ResponseEntity<Any> =
      runCatching {
            val antallBilder =
                testResultatDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
            val imageDetails =
                multipartFilesToImageDetails(testresultatId, antallBilder.size, bilder).getOrThrow()

            blobClient.uploadBilder(imageDetails).forEach { bildeResultat ->
              if (bildeResultat.isSuccess) {
                val opprettet = Instant.now()
                val bildeDetalj = bildeResultat.getOrThrow()
                testResultatDAO
                    .saveBilde(
                        testresultatId, bildeDetalj.fileName, bildeDetalj.thumbnailName, opprettet)
                    .getOrThrow()
              }
            }
          }
          .fold(
              { ResponseEntity.noContent().build() },
              {
                logger.error("Feil ved opplasting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @CacheEvict(value = ["bildeCache"], key = "#testresultatId")
  @DeleteMapping("{testresultatId}/{bildeId}")
  fun deleteBilde(@PathVariable testresultatId: Int, @PathVariable bildeId: Int) =
      runCatching {
            val bildeSti =
                testResultatDAO.getBildeSti(bildeId).getOrThrow()
                    ?: throw IllegalArgumentException("Fann ikkje bilde for bilde-id $bildeId")

            blobClient.deleteBilde(bildeSti.bilde).onFailure {
              logger.error("Kunne ikkje slette bilde frå blob storage", it)
              throw it
            }

            blobClient.deleteBilde(bildeSti.thumbnail).onFailure {
              blobClient.restoreBilde(bildeSti.bilde).onFailure { ex ->
                logger.error("Kunne ikkje gjenopprette bilde", ex)
              }
              logger.error("Kunne ikkje slette thumbnail frå blob storage")
              throw it
            }

            testResultatDAO.deleteBilde(bildeId).onFailure {
              logger.error("Kunne ikkje slette bilde frå database", it)
              blobClient.restoreBilde(bildeSti.bilde).onFailure { ex ->
                logger.error("Kunne ikkje gjenopprette bilde", ex)
              }
              blobClient.restoreBilde(bildeSti.thumbnail).onFailure { ex ->
                logger.error("Kunne ikkje gjenopprette bilde", ex)
              }

              throw it
            }

            ResponseEntity.noContent().build<Unit>()
          }
          .onFailure {
            logger.error("Kunne ikkje slette bilde", it)
            ResponseEntity.internalServerError().build<Unit>()
          }

  @Cacheable("bildeCache", key = "#testresultatId")
  @GetMapping("/{testresultatId}")
  fun getBildeListForTestresultat(@PathVariable testresultatId: Int): ResponseEntity<List<Bilde>> =
      runCatching {
            val paths = testResultatDAO.getBildePathsForTestresultat(testresultatId).getOrThrow()
            blobClient.getBildeStiList(paths)
          }
          .fold(
              { ResponseEntity.ok(it.getOrThrow()) },
              {
                logger.error("Feil ved opplasting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @CacheEvict(value = ["bildeCache"], allEntries = true)
  @Scheduled(fixedRateString = "\${blobstorage.sasTTL}")
  fun emptyBildeCache() {
    logger.info("Tømmer bildeCache")
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
  ): Result<List<CloudImageDetails>> {
    val allowedMIMETypes = listOf("jpg", "jpeg", "png", "bmp")

    return runCatching {
      bildeList.mapIndexed { index, bilde ->
        val originalFileName =
            bilde.originalFilename ?: throw IllegalArgumentException("Filnamn er tomt")
        val fileExtension = originalFileName.substringAfterLast('.')

        if (!allowedMIMETypes.contains(fileExtension)) {
          throw IllegalArgumentException(
              "$originalFileName har annen filtype enn ${allowedMIMETypes.joinToString(",")}")
        }

        val image = ImageIO.read(bilde.inputStream)
        val thumbnail = createThumbnailImage(image)

        val bildeIndex = indexOffset + index
        val newFileName = "${testresultatId}_${bildeIndex}.$fileExtension"
        val newFileNameThumb = "${testresultatId}_${bildeIndex}_thumb.$fileExtension"

        CloudImageDetails(image, thumbnail, newFileName, newFileNameThumb, fileExtension)
      }
    }
  }
}
