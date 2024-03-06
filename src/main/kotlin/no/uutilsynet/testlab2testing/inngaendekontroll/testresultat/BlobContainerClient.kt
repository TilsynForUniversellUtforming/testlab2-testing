package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.azure.storage.blob.BlobContainerClientBuilder
import java.io.ByteArrayInputStream
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "blobstorage")
data class BlobStorageProperties(val connection: String, val container: String)

@Component
class BlobContainerClient(private final val blobStorageProperties: BlobStorageProperties) {
  private val blobContainerClient =
      BlobContainerClientBuilder()
          .connectionString(blobStorageProperties.connection)
          .containerName(blobStorageProperties.container)
          .buildClient()

  fun uploadImage(imageData: ByteArrayInputStream, fileName: String) {
    val blobClient = blobContainerClient.getBlobClient(fileName)
    blobClient.upload(imageData, imageData.available().toLong(), true)
  }
}
