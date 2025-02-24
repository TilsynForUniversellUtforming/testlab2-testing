package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeRequest
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.KontrollDocumentation
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BildeServiceTest(@Autowired val bildeService: BildeService) {
  @MockitoSpyBean lateinit var imageStorageService: ImageStorageService

  @MockitoBean lateinit var testResultatDAO: TestResultatDAO

  @Test
  @DisplayName("Skal lagre bilder som blir lastet opp i databasen")
  fun saveUploaded() {
    val testresultatId = 1
    val expectedBilde = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val expectedThumbnail = createThumbnailImage(expectedBilde)

    val expectedBildeDetalj =
        BildeRequest(
            image = expectedBilde,
            thumbnail = expectedThumbnail,
            fileName = "1_0",
            fileExtension = "jpg")

    val bilde = listOf(MockMultipartFile("bilde", "bilde.jpg", "image/jpeg", mockImageByteArray()))
    val expectedImageDetails = listOf(expectedBildeDetalj)

    `when`(testResultatDAO.getBildePathsForTestresultat(testresultatId))
        .thenReturn(Result.success(emptyList()))

    `when`(testResultatDAO.getKontrollForTestresultat(testresultatId))
        .thenReturn(Result.success(KontrollDocumentation("Testkontroll", 1)))

    `when`(
            testResultatDAO.saveBilde(
                testresultatId,
                expectedBildeDetalj.fullFileName,
                expectedBildeDetalj.fullThumbnailName))
        .thenReturn(Result.success(1))

    `when`(imageStorageService.uploadBilder(anyList()))
        .thenReturn(expectedImageDetails.map { Result.success(it) })

    bildeService.createBilde(testresultatId, bilde)

    verify(imageStorageService, times(1)).uploadBilder(anyList())
    verify(testResultatDAO, times(1))
        .saveBilde(1, expectedBildeDetalj.fullFileName, expectedBildeDetalj.fullThumbnailName)
  }

  @Test
  @DisplayName("Bilder skal bli indeksert riktig i forhold til antall eksisterende")
  fun saveUploadedIndexed() {
    val testresultatId = 1
    val expectedBilde = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
    val expectedThumbnail = createThumbnailImage(expectedBilde)
    val numImages = 9
    val expectedIndex = numImages + 1
    val expectedFileName = "${testresultatId}_$expectedIndex"

    val expectedBildeDetalj =
        BildeRequest(
            image = expectedBilde,
            thumbnail = expectedThumbnail,
            fileName = expectedFileName,
            fileExtension = "jpg")

    val bilde = listOf(MockMultipartFile("bilde", "bilde.jpg", "image/jpeg", mockImageByteArray()))
    val expectedImageDetails = listOf(expectedBildeDetalj)

    `when`(testResultatDAO.getBildePathsForTestresultat(testresultatId))
        .thenReturn(
            Result.success(
                (0..numImages).map {
                  (BildeSti(1, "1_${it}.jpg", "1_${it}_thumb.jpg", Instant.now()))
                }))

    `when`(testResultatDAO.getKontrollForTestresultat(testresultatId))
        .thenReturn(Result.success(KontrollDocumentation("Testkontroll", 1)))

    `when`(
            testResultatDAO.saveBilde(
                testresultatId,
                expectedBildeDetalj.fullFileName,
                expectedBildeDetalj.fullThumbnailName))
        .thenReturn(Result.success(1))

    `when`(imageStorageService.uploadBilder(anyList()))
        .thenReturn(expectedImageDetails.map { Result.success(it) })

    bildeService.createBilde(testresultatId, bilde)

    verify(imageStorageService, times(1)).uploadBilder(anyList())
    verify(testResultatDAO, times(1))
        .saveBilde(1, expectedBildeDetalj.fullFileName, expectedBildeDetalj.fullThumbnailName)
  }

  private fun mockImageByteArray(): ByteArray {
    val baos = ByteArrayOutputStream()
    baos.use { os -> ImageIO.write(BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", os) }
    return baos.toByteArray()
  }

  private fun createThumbnailImage(originalImage: BufferedImage): BufferedImage {
    val imageTargetSize = 100
    val resultingImage =
        originalImage.getScaledInstance(imageTargetSize, imageTargetSize, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(imageTargetSize, imageTargetSize, BufferedImage.TYPE_INT_RGB)
    outputImage.graphics.drawImage(resultingImage, 0, 0, null)

    return outputImage
  }
}
