package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobContainerClientBuilder
import org.springframework.stereotype.Component

@Component
class BlobContainerClientFactoryImpl(private val blobStorageProperties: BlobStorageProperties) :
    BlobContainerClientFactory {
  override fun createBlobContainerClient(): BlobContainerClient {
    return BlobContainerClientBuilder()
        .connectionString(blobStorageProperties.connection)
        .containerName(blobStorageProperties.container)
        .buildClient()
  }
}
