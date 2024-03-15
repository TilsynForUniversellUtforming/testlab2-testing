package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.azure.storage.blob.BlobContainerClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.OffsetDateTime
import javax.imageio.ImageIO
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "blobstorage")
data class BlobStorageProperties(
    val connection: String,
    val account: String,
    val container: String,
    val sasDurationMinutes: Int,
)

@Component
class BlobContainerClient(private final val blobStorageProperties: BlobStorageProperties) {

  private val logger = LoggerFactory.getLogger(BlobContainerClient::class.java)

  private val blobContainerClient =
      BlobContainerClientBuilder()
          .connectionString(blobStorageProperties.connection)
          .containerName(blobStorageProperties.container)
          .buildClient()

  fun uploadImage(image: BufferedImage, fileName: String, fileExtension: String) {
    ByteArrayOutputStream().use { os ->
      ImageIO.write(image, fileExtension, os)
      upload(ByteArrayInputStream(os.toByteArray()), fileName)
    }
  }

  fun uploadImages(cloudImageDetails: List<CloudImageDetails>): List<Result<CloudImageDetails>> =
      cloudImageDetails.map { detail ->
        runCatching {
              val imagesToUpload =
                  listOf(detail.image to detail.fileName, detail.thumbnail to detail.thumbnailName)

              imagesToUpload.forEach { (image, fileName) ->
                uploadImage(image, fileName, detail.fileExtension)
              }
              detail
            }
            .onFailure { ex -> logger.error("Kunne ikkje laste opp bilde ${detail.fileName}", ex) }
      }

  private fun upload(data: ByteArrayInputStream, fileName: String) =
      runCatching {
            val blobClient = blobContainerClient.getBlobClient(fileName)
            blobClient.upload(data, data.available().toLong(), true)
          }
          .onFailure { logger.error("Kunne ikkje laste opp fil $fileName") }

  fun getImageUrls(cloudImagePathsList: List<CloudImagePaths>): Result<List<CloudImageUris>> =
      runCatching {
            val expiryTime =
                OffsetDateTime.now().plusMinutes(blobStorageProperties.sasDurationMinutes.toLong())
            val permission = BlobSasPermission().setReadPermission(true).setWritePermission(false)
            val sasValues = BlobServiceSasSignatureValues(expiryTime, permission)
            val sasToken = blobContainerClient.generateSas(sasValues)
            cloudImagePathsList.map { bilde ->
              CloudImageUris(
                  imageURI = toBlobUri(bilde.bilde, sasToken),
                  thumbnailURI = toBlobUri(bilde.thumbnail, sasToken),
              )
            }
          }
          .onFailure { logger.error("Kunne ikkje hente bilder", it) }

  private fun toBlobUri(filnamn: String, sasToken: String) =
      URI(
          "https://${blobStorageProperties.account}.blob.core.windows.net/screenshots/${filnamn}?$sasToken")
}
