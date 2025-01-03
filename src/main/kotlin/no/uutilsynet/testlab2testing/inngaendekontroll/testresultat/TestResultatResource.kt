package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser.ImportBody
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/testresultat")
class TestResultatResource(
    val testResultatDAO: TestResultatDAO,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val aggregeringService: AggregeringService,
    val brukarService: BrukarService,
    val bildeService: BildeService,
    val testresultatService: TestresultatService
) {
  val logger: Logger = getLogger(TestResultatResource::class.java)

  @PostMapping
  fun createTestResultat(
      @RequestBody createTestResultat: CreateTestResultat
  ): ResponseEntity<Unit> =
      runCatching {
            val brukar = brukarService.getCurrentUser()
            testResultatDAO.save(createTestResultat.copy(brukar = brukar)).getOrThrow()
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

  @PostMapping("/aggregert/{testgrunnlagId}")
  fun createAggregertResultat(@PathVariable testgrunnlagId: Int) =
      aggregeringService.saveAggregertResultat(testgrunnlagId)

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

  @GetMapping("/aggregert/{testgrunnlagId}")
  fun getAggregertResultat(@PathVariable testgrunnlagId: Int) =
      aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)

  @PostMapping("/import")
  fun importTestResultat(@RequestBody createTestResultat: ImportBody): ResponseEntity<Int> =
      runCatching {
            val brukar = brukarService.getCurrentUser()

            val testresultat =
                testresultatService
                    .importTestelement(
                        createTestResultat.testresultatBase, createTestResultat.xpathExpression)
                    .map { testResultatDAO.save(it.copy(brukar = brukar)).getOrThrow() }
            testresultat.size
          }
          .fold(
              { ResponseEntity.ok(it) },
              {
                logger.error("Feil ved oppretting av testresultat", it)
                ResponseEntity.internalServerError().build()
              })

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()
}
