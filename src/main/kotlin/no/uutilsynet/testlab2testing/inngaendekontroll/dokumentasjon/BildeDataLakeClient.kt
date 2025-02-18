package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.file.datalake.DataLakeFileSystemClient
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder
import com.azure.storage.file.datalake.sas.DataLakeServiceSasSignatureValues
import com.azure.storage.file.datalake.sas.PathSasPermission
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime

@Component
class BildeDataLakeClient(private val blobStorageProperties: BlobStorageProperties) :
    ImageStorageClient {

  private val dataLakeFileSystemClient = createDatalakeClient()

  private final fun createDatalakeClient(): DataLakeFileSystemClient {
    return DataLakeServiceClientBuilder()
        .connectionString(blobStorageProperties.connection)
        .buildClient()
        .getFileSystemClient(blobStorageProperties.container)
  }

  override fun uploadToStorage(data: ByteArrayInputStream, fileName: String): Result<Unit> {
    return runCatching {
      val fileClient = dataLakeFileSystemClient.getFileClient(fileName)
      fileClient.upload(data, data.available().toLong(), true)
      fileClient.flush(data.available().toLong(), true)
    }
  }

  override fun toBlobUri(filnamn: String, sasToken: String): URI {
    return URI(
        "https://${blobStorageProperties.account}.blob.core.windows.net/${blobStorageProperties.container}/${filnamn}?$sasToken")
  }

  fun generateSas(sasValues: DataLakeServiceSasSignatureValues): String {
    return dataLakeFileSystemClient.generateSas(sasValues)
  }

  override fun getSasToken(): String {
    val expiryTime =
        OffsetDateTime.ofInstant(
            Instant.now().plusMillis(blobStorageProperties.sasttl.toLong()), ZONEID_OSLO)
    val permission = PathSasPermission().setReadPermission(true).setWritePermission(false)

    val sasValues = DataLakeServiceSasSignatureValues(expiryTime, permission)
    return generateSas(sasValues)
  }

  override fun deleteBilde(imagePath: String): Result<Boolean> {
    return runCatching { dataLakeFileSystemClient.getFileClient(imagePath).deleteIfExists() }
  }

  override fun restoreBilde(imagePath: String): Result<Unit> {
    return runCatching {
      val dataLakeFileClient = dataLakeFileSystemClient.getFileClient(imagePath)

      getDeletionId(imagePath)
          .onSuccess {
            dataLakeFileSystemClient.undeletePath(
                dataLakeFileClient.filePath, getDeletionId(imagePath).getOrThrow())
          }
          .onFailure { throw RuntimeException("Kunne ikkje hente restore fil $imagePath") }
    }
  }

  fun getDeletionId(imagePath: String): Result<String> {
    return runCatching {
      val dataLakeFileClient = dataLakeFileSystemClient.getFileClient(imagePath)
      for (item in dataLakeFileSystemClient.listDeletedPaths()) {
        if (item.path == dataLakeFileClient.filePath) {
          item.deletionId
        }
      }
      throw RuntimeException("Kunne ikkje hente slettings-id for filen")
    }
  }
}
