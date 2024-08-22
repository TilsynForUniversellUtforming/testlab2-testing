package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2testing.dto.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontrollDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

@RestController
@RequestMapping("/testgrunnlag/kontroll")
class TestgrunnlagKontrollResource(
    val testgrunnlagDAO: TestgrunnlagKontrollDAO,
    val testResultatDAO: TestResultatDAO
) {

  val logger: Logger = LoggerFactory.getLogger(TestgrunnlagKontrollResource::class.java)

  @GetMapping
  fun getTestgrunnlagList(
      @RequestParam kontrollId: Int,
      @RequestParam loeysingId: Int?
  ): ResponseEntity<List<TestgrunnlagKontroll>> {
    return ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).toList())
  }

  @PostMapping
  fun createTestgrunnlag(@RequestBody testgrunnlag: NyttTestgrunnlag): ResponseEntity<Int> {
    logger.info(
        "Opprett testgrunnlag for sak ${testgrunnlag.kontrollId} og loeysinger ${testgrunnlag.sideutval.map { it.loeysingId }}")

    return runCatching { testgrunnlagDAO.createTestgrunnlag(testgrunnlag).getOrThrow() }
        .fold(
            onSuccess = { id ->
              logger.info("Oppretta testgrunnlag med id $id")
              ResponseEntity.created(location(id)).build()
            },
            onFailure = {
              logger.error("Feil ved oppretting av testgrunnlag", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @PostMapping("retest")
  fun createRetest(@RequestBody retest: Retest): ResponseEntity<Unit> =
      runCatching {
            val (originalTestgrunnlagId, loeysingId) = retest

            val originaltTestgrunnlag =
                testgrunnlagDAO.getTestgrunnlag(originalTestgrunnlagId).getOrElse {
                  logger.error(
                      "Klarte ikkje å henta testgrunnlag for id $originalTestgrunnlagId: $it")
                  throw it
                }

            val originalResultatList =
                testResultatDAO.getManyResults(originalTestgrunnlagId).getOrElse {
                  logger.error(
                      "Klarte ikkje å henta testresultat for testgrunnlag $originalTestgrunnlagId: $it")
                  throw it
                }

            val (originalBrotResultat, originalAnnaResultat) =
                originalResultatList.partition {
                  it.testgrunnlagId == originalTestgrunnlagId &&
                      it.loeysingId == loeysingId &&
                      it.elementResultat != null &&
                      it.elementResultat == TestresultatUtfall.brot
                }

            if (originalBrotResultat.isEmpty()) {
              logger.info("Ingen resultat med brot, kan ikkje køyre retest")
              throw IllegalArgumentException("Ingen resultat med brot, kan ikkje køyre retest")
            }

            val kontrollId = originaltTestgrunnlag.kontrollId
            val nyttTestgrunnlag =
                NyttTestgrunnlag(
                    kontrollId = kontrollId,
                    namn = "Retest for kontroll $kontrollId",
                    type = TestgrunnlagType.RETEST,
                    sideutval =
                        originaltTestgrunnlag.sideutval.filter { it.loeysingId == loeysingId },
                    testregelIdList = originaltTestgrunnlag.testreglar.map { it.id })

            val nyttTestgrunnlagId =
                testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag).getOrElse {
                  logger.error(
                      "Kunne ikkje opprette testgrunnlag for løysing $loeysingId i kontroll $kontrollId med opprinnelig testgrunnlag $originalTestgrunnlagId",
                      it)
                  throw it
                }

            val created = Instant.now()

            // Kopier svar fra test med brot
            val brotResultatList =
                originalBrotResultat.map {
                  ResultatManuellKontrollBase(
                      testgrunnlagId = nyttTestgrunnlagId,
                      loeysingId = loeysingId,
                      testregelId = it.testregelId,
                      sideutvalId = it.sideutvalId,
                      brukar = it.brukar,
                      elementOmtale = it.elementOmtale,
                      elementResultat = it.elementResultat,
                      elementUtfall = it.elementUtfall,
                      svar = it.svar,
                      testVartUtfoert = null,
                      kommentar = null,
                      sistLagra = created,
                  )
                }

            // Bruk de andre resultatene som de er
            val annaResultatList =
                originalAnnaResultat.map {
                  ResultatManuellKontrollBase(
                      testgrunnlagId = nyttTestgrunnlagId,
                      loeysingId = loeysingId,
                      testregelId = it.testregelId,
                      sideutvalId = it.sideutvalId,
                      brukar = it.brukar,
                      elementOmtale = it.elementOmtale,
                      elementResultat = it.elementResultat,
                      elementUtfall = it.elementUtfall,
                      svar = it.svar,
                      testVartUtfoert = it.testVartUtfoert,
                      status = it.status,
                      kommentar = it.kommentar,
                      sistLagra = created,
                  )
                }

            val retestTestresultat = brotResultatList + annaResultatList

            retestTestresultat.forEach { resultat ->
              testResultatDAO.createRetest(resultat).getOrElse {
                logger.error("Kunne ikkje oppdatere resultat for retest", it)
                throw it
              }
            }

            nyttTestgrunnlagId
          }
          .fold(
              onSuccess = { testgrunnlagId ->
                ResponseEntity.created(location(testgrunnlagId)).build()
              },
              onFailure = {
                logger.error("Kunne ikkje lage retest av testresultat", it)
                ResponseEntity.internalServerError().build()
              })

  @GetMapping("list/{kontrollId}")
  fun listTestgrunnlagForKontroll(
      @PathVariable kontrollId: Int
  ): ResponseEntity<List<TestgrunnlagKontroll>> =
      ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).toList())

  @GetMapping("/{id}")
  fun getTestgrunnlag(@PathVariable id: Int): ResponseEntity<TestgrunnlagKontroll> {
    return testgrunnlagDAO
        .getTestgrunnlag(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @PutMapping("/{id}")
  fun updateTestgrunnlag(
      @PathVariable id: Int,
      @RequestBody testgrunnlag: TestgrunnlagKontroll
  ): ResponseEntity<TestgrunnlagKontroll> {
    require(testgrunnlag.id == id) { "id i URL-en og id er ikkje den same" }
    return testgrunnlagDAO
        .updateTestgrunnlag(testgrunnlag)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @DeleteMapping("/{id}")
  fun deleteTestgrunnlag(@PathVariable id: Int): ResponseEntity<Unit> {
    return runCatching { testgrunnlagDAO.deleteTestgrunnlag(id) }
        .fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
              logger.error("Feil ved sletting av testgrunnlag", it)
              ResponseEntity.notFound().build()
            })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentServletMapping()
          .path("testgrunnlag/kontroll/$id")
          .buildAndExpand(id)
          .toUri()

  fun TestgrunnlagList.toList(): List<TestgrunnlagKontroll> {
    return listOf<TestgrunnlagKontroll>(this.opprinneligTest) + this.restestar
  }

  data class Retest(val originalTestgrunnlagId: Int, val loeysingId: Int)
}
