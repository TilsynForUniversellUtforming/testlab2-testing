package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ImageStorageService(
    private val blobStorageProperties: BlobStorageProperties,
    @Qualifier("bildeDataLakeClient") val imageStorageClient: ImageStorageClient
) {

  private val logger = LoggerFactory.getLogger(ImageStorageService::class.java)

  private val bildepath = "/bilder/sti/"

  fun uploadBilder(cloudImageDetails: List<BildeRequest>): List<Result<BildeRequest>> =
      cloudImageDetails.map { detail ->
        println("Uploading image ${detail.fileName}")
        runCatching {
              val imagesToUpload =
                  listOf(
                      detail.image to detail.fullFileName,
                      detail.thumbnail to detail.fullThumbnailName)

              imagesToUpload.forEach { (image, fileName) ->
                uploadSingleBilde(image, fileName, detail.fileExtension)
              }
              detail
            }
            .onFailure { ex ->
              logger.error("Kunne ikkje laste opp bilde ${detail.fileName}", ex.message)
              throw ex
            }
      }

  fun getBildeStiList(bildeStiList: List<BildeSti>): Result<List<Bilde>> =
      runCatching {
            imageStorageClient.getSasToken()
            bildeStiList.map { bilde ->
              Bilde(
                  id = bilde.id,
                  bildeURI = URI(blobStorageProperties.eksternalhost + bildepath + bilde.bilde),
                  thumbnailURI =
                      URI(blobStorageProperties.eksternalhost + bildepath + bilde.thumbnail),
                  opprettet = bilde.opprettet)
            }
          }
          .onFailure { logger.error("Kunne ikkje hente bilder", it) }

  private fun uploadSingleBilde(image: BufferedImage, fileName: String, fileExtension: String) {
    ByteArrayOutputStream().use { os ->
      ImageIO.write(image, fileExtension, os)
      imageStorageClient.uploadToStorage(ByteArrayInputStream(os.toByteArray()), fileName)
    }
  }

  fun getBildeSti(path: String): HttpURLConnection {
    val sasToken = imageStorageClient.getSasToken()
    val storageUri = imageStorageClient.toBlobUri(path, sasToken)
    return storageUri.toURL().openConnection(Proxy.NO_PROXY) as HttpURLConnection
  }

  fun deleteBilde(bildeSti: String) = imageStorageClient.deleteBilde(bildeSti)

  fun restoreBilde(bildeSti: String) = imageStorageClient.restoreBilde(bildeSti)
}
