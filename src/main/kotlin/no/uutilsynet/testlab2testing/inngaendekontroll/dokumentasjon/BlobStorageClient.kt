package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
class BlobStorageClient(
    private val blobStorageProperties: BlobStorageProperties,
    blobContainerClientFactory: BlobContainerClientFactory,
) {

  private val logger = LoggerFactory.getLogger(BlobStorageClient::class.java)

  private val blobContainerClient = blobContainerClientFactory.createBlobContainerClient()

  private val bildepath = "/bilder/sti/"

  fun uploadBilder(cloudImageDetails: List<BildeRequest>): List<Result<BildeRequest>> =
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

  fun deleteBilde(imagePath: String) = runCatching {
    val blobClient = blobContainerClient.getBlobClient(imagePath)
    blobClient.deleteIfExists()
  }

  fun restoreBilde(imagePath: String) = runCatching {
    val blobClient = blobContainerClient.getBlobClient(imagePath)
    blobClient.undelete()
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

  private fun getSasToken(): String {
    val expiryTime =
        OffsetDateTime.ofInstant(
            Instant.now().plusMillis(blobStorageProperties.sasttl.toLong()), ZONEID_OSLO)
    val permission = BlobSasPermission().setReadPermission(true).setWritePermission(false)
    val sasValues = BlobServiceSasSignatureValues(expiryTime, permission)
    val sasToken = blobContainerClient.generateSas(sasValues)
    return sasToken
  }

  fun getBildeSti(path: String): HttpURLConnection {
    val sasToken = getSasToken()
    val storageUri = toBlobUri(path, sasToken)
    println(storageUri)
    return storageUri.toURL().openConnection(Proxy.NO_PROXY) as HttpURLConnection
  }

  private fun uploadSingleBilde(image: BufferedImage, fileName: String, fileExtension: String) {
    ByteArrayOutputStream().use { os ->
      ImageIO.write(image, fileExtension, os)
      uploadToStorage(ByteArrayInputStream(os.toByteArray()), fileName)
    }
  }

  private fun uploadToStorage(data: ByteArrayInputStream, fileName: String) =
      runCatching {
            val blobClient = blobContainerClient.getBlobClient(fileName)
            blobClient.upload(data, data.available().toLong(), true)
          }
          .onFailure { logger.error("Kunne ikkje laste opp fil $fileName") }

  private fun toBlobUri(filnamn: String, sasToken: String) =
      URI(
          "https://${blobStorageProperties.account}.blob.core.windows.net/${blobStorageProperties.container}/${filnamn}?$sasToken")
}

@ConfigurationProperties(prefix = "server")
data class ServerProperties(
    val port: Int,
)
