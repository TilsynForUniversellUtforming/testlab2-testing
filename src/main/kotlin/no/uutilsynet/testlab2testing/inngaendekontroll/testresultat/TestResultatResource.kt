package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutomaticTestingService
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutotesterPayload
import no.uutilsynet.testlab2testing.testregel.TestregelCache
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.Instant

@RestController
@RequestMapping("/testresultat")
class TestResultatResource(
    val testResultatDAO: TestResultatDAO,
    val brukarService: BrukarService,
    val bildeService: BildeService,
    val automaticTestingService: AutomaticTestingService,
    val testregelCache: TestregelCache,
) {
  val logger: Logger = getLogger(TestResultatResource::class.java)

  @PostMapping
  fun createTestResultat(
      @RequestBody createTestResultat: CreateTestResultat
  ): ResponseEntity<Unit> =
      runCatching {
            val brukar = brukarService.getCurrentUser()

          val resultToSave =
              if (isTestregelAutomatic(createTestResultat.testregelId)) {
                  val newResult = getResultAutomatic(createTestResultat)
                  if (newResult.elementResultat == null) {
                      createTestResultat
                  } else {
                      newResult
              }

              } else {
                createTestResultat
              }
          logger.info(resultToSave.toString())



          testResultatDAO.save(resultToSave.copy(brukar = brukar)).getOrThrow()
          1
          }
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

  @GetMapping
  fun getManyResults(
      @RequestParam testgrunnlagId: Int
  ): ResponseEntity<Map<String, List<ResultatManuellKontroll>>> {
    return testResultatDAO
        .getManyResults(testgrunnlagId)
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
    val brukar = brukarService.getCurrentUser()

    return testResultatDAO
        .update(testResultat.copy(brukar = brukar))
        .fold(
            onSuccess = { ResponseEntity.ok().build() },
            onFailure = {
              logger.error("Feil ved oppdatering av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }



  @DeleteMapping("/{id}")
  fun deleteTestResultat(@PathVariable id: Int): ResponseEntity<Unit> =
      runCatching {
            logger.info("Sletter testresultat med id $id")
            val resultat = testResultatDAO.getTestResultat(id).getOrThrow()
            require(resultat.status != ResultatManuellKontrollBase.Status.Ferdig) {
              "Resultat er ferdig og kan ikke slettes"
            }
            testResultatDAO.delete(id).getOrThrow()
            bildeService.deleteBilder(id).getOrThrow()
          }
          .fold(
              { ResponseEntity.ok().build() },
              {
                if (it is IllegalArgumentException) {
                  logger.error("Testresultat har status ferdig", it)
                  ResponseEntity.badRequest().build()
                } else {
                  logger.error("Feil ved sletting av testresultat eller bilde", it)
                  ResponseEntity.internalServerError().build()
                }
              })




    fun getResultAutomatic(ceateTestResultat: CreateTestResultat): CreateTestResultat {
        val testregel = testregelCache.getTestregelById(ceateTestResultat.testregelId)
        return automaticTestingService.testElement(
            AutotesterPayload(
                htmlElement = ceateTestResultat.elementOmtaleHtml ?: "",
                qualwebRule = testregel.testregelId
            )
        ).fold(
            onSuccess = { ceateTestResultat.copy(
                elementResultat = TestresultatUtfall.valueOf(it.elementResultat),
                elementUtfall = it.elementUtfall
            ) },
            onFailure = {
                ceateTestResultat
            }
        )


    }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

    private fun isTestregelAutomatic(testregelId: Int): Boolean {
        val testregel = testregelCache.getTestregelById(testregelId)
        return testregel.modus == TestregelModus.automatisk
    }


  data class CreateTestResultat(
      val testgrunnlagId: Int,
      val loeysingId: Int,
      val testregelId: Int,
      val sideutvalId: Int,
      val brukar: Brukar?,
      val elementOmtale: String? = null,
      val elementOmtaleHtml: String? = null,
      val elementResultat: TestresultatUtfall? = null,
      val elementUtfall: String? = null,
      val testVartUtfoert: Instant? = null,
      val kommentar: String? = null,
  )
}
