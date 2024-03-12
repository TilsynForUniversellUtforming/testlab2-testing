package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.azure.storage.blob.BlobContainerClientBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "blobstorage")
data class BlobStorageProperties(val connection: String, val container: String)

@Component
class BlobContainerClient(private final val blobStorageProperties: BlobStorageProperties) {

  private val logger = LoggerFactory.getLogger(BlobContainerClient::class.java)

  private val blobContainerClient =
      BlobContainerClientBuilder()
          .connectionString(blobStorageProperties.connection)
          .containerName(blobStorageProperties.container)
          .buildClient()

  fun uploadImages(imageDetails: List<ImageDetail>): List<Result<ImageDetail>> =
      imageDetails.map { detail ->
        runCatching {
              ByteArrayOutputStream().use { os ->
                ImageIO.write(detail.image, detail.fileExtension, os)
                val imageBytes = os.toByteArray()
                upload(ByteArrayInputStream(imageBytes), detail.fileName)
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

  fun download(fileName: String): Result<ByteArray> = runCatching {
    val blobClient = blobContainerClient.getBlobClient(fileName)
    if (!blobClient.exists()) {
      throw IllegalArgumentException("$fileName finns ikkje")
    }

    ByteArrayOutputStream().use { output ->
      blobClient.downloadStream(output)
      output.toByteArray()
    }
  }
}
