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

abstract class ImageStorageClient(private val blobStorageProperties: BlobStorageProperties) {

  private val logger = LoggerFactory.getLogger(ImageStorageClient::class.java)

  private val bildepath = "/bilder/sti/"

  open fun uploadBilder(cloudImageDetails: List<BildeRequest>): List<Result<BildeRequest>> =
      cloudImageDetails.map { detail ->
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
            getSasToken()
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
      uploadToStorage(ByteArrayInputStream(os.toByteArray()), fileName)
    }
  }

  fun getBildeSti(path: String): HttpURLConnection {
    val sasToken = getSasToken()
    val storageUri = toBlobUri(path, sasToken)
    return storageUri.toURL().openConnection(Proxy.NO_PROXY) as HttpURLConnection
  }

  abstract fun uploadToStorage(data: ByteArrayInputStream, fileName: String): Result<Unit>

  abstract fun toBlobUri(filnamn: String, sasToken: String): URI

  abstract fun getSasToken(): String

  abstract fun deleteBilde(imagePath: String): Result<Boolean>

  abstract fun restoreBilde(imagePath: String): Result<Unit>
}
