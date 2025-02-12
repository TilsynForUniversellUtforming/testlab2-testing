package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import org.springframework.stereotype.Component

@Component
class BlobStorageClient(
    private val blobStorageProperties: BlobStorageProperties,
    blobContainerClientFactory: BlobContainerClientFactory,
) : ImageStorageClient(blobStorageProperties) {

  private val blobContainerClient = blobContainerClientFactory.createBlobContainerClient()

  override fun uploadBilder(cloudImageDetails: List<BildeRequest>): List<Result<BildeRequest>> {
    return super.uploadBilder(cloudImageDetails)
  }

  override fun deleteBilde(imagePath: String) = runCatching {
    val blobClient = blobContainerClient.getBlobClient(imagePath)
    blobClient.deleteIfExists()
  }

  override fun restoreBilde(imagePath: String): Result<Unit> = runCatching {
    val blobClient = blobContainerClient.getBlobClient(imagePath)
    blobClient.undelete()
  }

  override fun uploadToStorage(data: ByteArrayInputStream, fileName: String): Result<Unit> =
      runCatching {
        val blobClient = blobContainerClient.getBlobClient(fileName)
        blobClient.upload(data, data.available().toLong(), true)
      }

  override fun toBlobUri(filnamn: String, sasToken: String) =
      URI(
          "https://${blobStorageProperties.account}.blob.core.windows.net/${blobStorageProperties.container}/${filnamn}?$sasToken")

  fun generateSas(sasValues: BlobServiceSasSignatureValues): String {
    return blobContainerClient.generateSas(sasValues)
  }

  override fun getSasToken(): String {
    val expiryTime =
        OffsetDateTime.ofInstant(
            Instant.now().plusMillis(blobStorageProperties.sasttl.toLong()), ZONEID_OSLO)
    val permission = BlobSasPermission().setReadPermission(true).setWritePermission(false)
    val sasValues = BlobServiceSasSignatureValues(expiryTime, permission)
    return generateSas(sasValues)
  }
}
