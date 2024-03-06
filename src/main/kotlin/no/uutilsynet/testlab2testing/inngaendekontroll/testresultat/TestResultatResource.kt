package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.imageio.ImageIO
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.brukar.Brukar
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/testresultat")
class TestResultatResource(
    val testResultatDAO: TestResultatDAO,
    val aggregeringService: AggregeringService,
    val blobContainerClient: BlobContainerClient,
) {
  val logger: Logger = getLogger(TestResultatResource::class.java)

  @PostMapping
  fun createTestResultat(
      @RequestBody createTestResultat: CreateTestResultat
  ): ResponseEntity<Unit> =
      runCatching { testResultatDAO.save(createTestResultat).getOrThrow() }
          .fold(
              { id -> ResponseEntity.created(location(id)).build() },
              {
                logger.error("Feil ved oppretting av testresultat", it)
                ResponseEntity.internalServerError().build()
              })

  @GetMapping("/{id}")
  fun getOneResult(@PathVariable id: Int): ResponseEntity<ResultatManuellKontroll> {
    return testResultatDAO
        .getTestResultat(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = {
              logger.error("Feil ved henting av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @GetMapping("")
  fun getManyResults(
      @RequestParam sakId: Int
  ): ResponseEntity<Map<String, List<ResultatManuellKontroll>>> {
    return testResultatDAO
        .getManyResults(sakId)
        .fold(
            onSuccess = { ResponseEntity.ok(mapOf("resultat" to it)) },
            onFailure = {
              logger.error("Feil ved henting av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @PutMapping("/{id}")
  fun updateTestResultat(
      @PathVariable id: Int,
      @RequestBody testResultat: ResultatManuellKontroll
  ): ResponseEntity<Unit> {
    require(testResultat.id == id) { "id i URL-en og id i dei innsendte dataene er ikkje den same" }
    return testResultatDAO
        .update(testResultat)
        .fold(
            onSuccess = { ResponseEntity.ok().build() },
            onFailure = {
              logger.error("Feil ved oppdatering av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @PostMapping("/aggregert/{testgrunnlagId}")
  fun createAggregertResultat(@PathVariable testgrunnlagId: Int) =
      testResultatDAO.saveAggregertResultatTestregel(testgrunnlagId)

  @DeleteMapping("/{id}")
  fun deleteTestResultat(@PathVariable id: Int): ResponseEntity<Unit> {
    logger.info("Sletter testresultat med id $id")
    val resultat = testResultatDAO.getTestResultat(id).getOrThrow()
    return if (resultat.status == ResultatManuellKontroll.Status.Ferdig) {
      ResponseEntity.badRequest().build()
    } else {
      testResultatDAO
          .delete(id)
          .fold(
              onSuccess = { ResponseEntity.ok().build() },
              onFailure = {
                logger.error("Feil ved sletting av testresultat", it)
                ResponseEntity.internalServerError().build()
              })
    }
  }

  @GetMapping("/aggregert/{testgrunnlagId}")
  fun getAggregertResultat(@PathVariable testgrunnlagId: Int) =
      aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)

  @PostMapping("/bilder")
  fun createBilder(@RequestParam("bilde") bilde: MultipartFile) = runCatching {
    val image = ImageIO.read(bilde.inputStream)
    val thumbnail = createThumbnailImage(image)

    val originalFileName =
        bilde.originalFilename ?: throw IllegalArgumentException("Namn på bilete er tomt")
    val fileExtension = originalFileName.substringAfterLast('.')
    val fileName = originalFileName.substringBeforeLast('.')

    val newFileName = "$fileName.$fileExtension"
    val newFileNameThumb = "${fileName}_thumb.$fileExtension"

    bilde.inputStream.use { inputStream ->
      val originalImageBytes = inputStream.readAllBytes()
      blobContainerClient.uploadImage(ByteArrayInputStream(originalImageBytes), newFileName)
    }

    ByteArrayOutputStream().use { os ->
      ImageIO.write(thumbnail, fileExtension, os)
      val thumbBytes = os.toByteArray()
      blobContainerClient.uploadImage(ByteArrayInputStream(thumbBytes), newFileNameThumb)
    }
  }

  private fun createThumbnailImage(originalImage: BufferedImage): BufferedImage {
    val imageTargetSize = 150
    val resultingImage =
        originalImage.getScaledInstance(imageTargetSize, imageTargetSize, Image.SCALE_DEFAULT)
    val outputImage = BufferedImage(imageTargetSize, imageTargetSize, BufferedImage.TYPE_INT_RGB)
    outputImage.graphics.drawImage(resultingImage, 0, 0, null)

    return outputImage
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class CreateTestResultat(
      val sakId: Int,
      val loeysingId: Int,
      val testregelId: Int,
      val nettsideId: Int,
      val brukar: Brukar,
      val elementOmtale: String? = null,
      val elementResultat: String? = null,
      val elementUtfall: String? = null,
      val testVartUtfoert: Instant? = null
  )
}
