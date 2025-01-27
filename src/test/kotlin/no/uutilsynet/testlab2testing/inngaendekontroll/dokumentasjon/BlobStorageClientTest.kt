package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import java.awt.image.BufferedImage
import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class BlobStorageClientTest {

  private val expectedSasToken = "sasToken123"
  private val mockBlobContainerClientFactory = mock(BlobContainerClientFactory::class.java)
  private val mockBlobContainerClient: BlobContainerClient = mock(BlobContainerClient::class.java)
  private val mockBlobClient: BlobClient = mock(BlobClient::class.java)
  private val mockBlockStorageProperties =
      BlobStorageProperties(
          "connection",
          "account",
          "container",
          1,
          "localhost",
      )

  private val mockServerProperties = mock(ServerProperties::class.java)

  @BeforeEach
  fun setup() {
    `when`(mockBlobContainerClientFactory.createBlobContainerClient())
        .thenReturn(mockBlobContainerClient)

    `when`(mockBlobContainerClient.generateSas(any(BlobServiceSasSignatureValues::class.java)))
        .thenReturn(expectedSasToken)
    `when`(mockBlobContainerClient.getBlobClient(anyString())).thenReturn(mockBlobClient)

    `when`(mockBlobClient.deleteIfExists()).thenReturn(true)
  }

  @Test
  fun `getImageUrls skal returnere en liste med lenker med SAS-tokens`() {
    val bildeName = "1_0.png"
    val thumbName = "1_0_thumb.png"

    val bildeStiList = listOf(BildeSti(1, bildeName, thumbName, Instant.now()))

    val blobStorageClient =
        BlobStorageClient(mockBlockStorageProperties, mockBlobContainerClientFactory)

    val result = blobStorageClient.getBildeStiList(bildeStiList)

    assertTrue(result.isSuccess)
    val firstResult = result.getOrNull()?.first()
    Assertions.assertNotNull(firstResult)
    val bilde = firstResult!!

    assertTrue(
        bilde.bildeURI.toString() ==
            "https://${mockBlockStorageProperties.account}.blob.core.windows.net/${mockBlockStorageProperties.container}/$bildeName?$expectedSasToken")
    assertTrue(
        bilde.thumbnailURI.toString() ==
            "https://${mockBlockStorageProperties.account}.blob.core.windows.net/${mockBlockStorageProperties.container}/$thumbName?$expectedSasToken")
  }

  @Test
  fun `uploadImages skal laste opp bilder og thumbnails`() {
    val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val thumbnail = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val cloudImageDetails = listOf(BildeRequest(image, thumbnail, "1_0", "png"))

    val blobStorageClient =
        BlobStorageClient(mockBlockStorageProperties, mockBlobContainerClientFactory)

    val results = blobStorageClient.uploadBilder(cloudImageDetails)

    verify(mockBlobContainerClient, times(cloudImageDetails.size * 2)).getBlobClient(anyString())

    assertTrue(results.all { it.isSuccess })
    assertTrue(results.map { it.getOrNull() }.containsAll(cloudImageDetails))
  }

  @Test
  fun `deleteImage skal bruke deleteIfExists`() {
    val imagePath = "1_0.png"
    val blobStorageClient =
        BlobStorageClient(mockBlockStorageProperties, mockBlobContainerClientFactory)

    val result = blobStorageClient.deleteBilde(imagePath)

    verify(mockBlobClient).deleteIfExists()
    assertTrue(result.isSuccess)
  }

  @Test
  fun `restoreImage skal bruke undelete`() {
    val imagePath = "1_0.png"
    val blobStorageClient =
        BlobStorageClient(mockBlockStorageProperties, mockBlobContainerClientFactory)

    val result = blobStorageClient.restoreBilde(imagePath)

    verify(mockBlobClient).undelete()
    assertTrue(result.isSuccess)
  }
}
