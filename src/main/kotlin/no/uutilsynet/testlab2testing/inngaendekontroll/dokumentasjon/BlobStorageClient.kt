package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BlobStorageClient(
    private val blobStorageProperties: BlobStorageProperties,
    blobContainerClientFactory: BlobContainerClientFactory
) {

  private val logger = LoggerFactory.getLogger(BlobStorageClient::class.java)

  private val blobContainerClient = blobContainerClientFactory.createBlobContainerClient()

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
            .onFailure { ex -> logger.error("Kunne ikkje laste opp bilde ${detail.fileName}", ex) }
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
            val expiryTime =
                OffsetDateTime.ofInstant(
                    Instant.now().plusMillis(blobStorageProperties.sasTTL.toLong()),
                    ZoneId.of("Europe/Oslo"))
            val permission = BlobSasPermission().setReadPermission(true).setWritePermission(false)
            val sasValues = BlobServiceSasSignatureValues(expiryTime, permission)
            val sasToken = blobContainerClient.generateSas(sasValues)
            bildeStiList.map { bilde ->
              Bilde(
                  id = bilde.id,
                  bildeURI = toBlobUri(bilde.bilde, sasToken),
                  thumbnailURI = toBlobUri(bilde.thumbnail, sasToken),
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
