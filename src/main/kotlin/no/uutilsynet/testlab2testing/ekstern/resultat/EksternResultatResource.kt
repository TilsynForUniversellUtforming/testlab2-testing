package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
  @Autowired val eksternResultatDAO: EksternResultatDAO,
  @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
  @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
  @Autowired val resultatService: ResultatService,
  @Autowired val kravregisterClient: KravregisterClient,
) {

  @GetMapping
  fun findTestForOrgNr(
    @RequestParam("orgnr") orgnr: String
  ): ResponseEntity<TestListElementEkstern?> {
    logger.debug("Henter tester for orgnr $orgnr")

    val loeysingList = loeysingsRegisterClient.search(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.info("Fann ingen løysingar for verkemd med orgnr $orgnr")
      return ResponseEntity.notFound().build<TestListElementEkstern>()
    }

    val loeysingIdList = loeysingList.map { it.id }

    val testList = eksternResultatDAO.getTestsForLoeysingIds(loeysingIdList)
    if (testList.isEmpty()) {
      logger.info("Fann ingen gyldige testar for orgnr $orgnr")
      return ResponseEntity.notFound().build<TestListElementEkstern>()
    }

    val verksemd = loeysingsRegisterClient.searchVerksemd(orgnr).getOrThrow().firstOrNull()

    val testEksternList =
      testList
        .flatMap { test ->
          val kontrollResult =
            resultatService.getKontrollResultat(test.kontrollId).first { result ->
              result.testType == OPPRINNELEG_TEST
            }

          kontrollResult.loeysingar.map { loeysingResult ->
            test.toListElement(loeysingResult.loeysingNamn, loeysingResult.score)
          }
        }
        .sortedBy { it.publisert }

    return ResponseEntity.ok(
      TestListElementEkstern(
        verksemd =
        VerksemdEkstern(
          namn = verksemd?.namn ?: orgnr,
          organisasjonsnummer = verksemd?.organisasjonsnummer ?: orgnr,
        ),
        testList = testEksternList
      )
    )
  }

  @GetMapping("{rapportId}")
  fun getResultatRapport(
    @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatOversiktLoeysingEkstern>> {
    val testgrunnlagLoeysing =
      eksternResultatDAO.findTestgrunnlagLoeysingFromRapportId((rapportId))
        ?: return ResponseEntity.badRequest().build()

    val results =
      resultatService.getTestgrunnlagLoeysingResultat(
        testgrunnlagLoeysing.testgrunnlagId, testgrunnlagLoeysing.loeysingId
      )
    if (results.isNullOrEmpty()) {
      return ResponseEntity.badRequest().build()
    }

    return ResponseEntity.ok(results.map { it.toResultatOversiktLoeysingEkstern() })
  }

  @GetMapping("{rapportId}/tema")
  fun getResultatPrTema(
    @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatTemaEkstern>> {
    val testgrunnlagLoeysing =
      eksternResultatDAO.findTestgrunnlagLoeysingFromRapportId((rapportId))
        ?: return ResponseEntity.badRequest().build()

    val testgrunnlag =
      testgrunnlagDAO.getTestgrunnlag(testgrunnlagLoeysing.testgrunnlagId).getOrElse {
        return ResponseEntity.badRequest().build()
      }

    val resultatTema = resultatService.getResultatPrTema(testgrunnlag.kontrollId, null, null, null)
    if (resultatTema.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val resultatTemaEkstern = resultatTema.map { it.toResultatTemaEkstern() }

    return ResponseEntity.ok(resultatTemaEkstern)
  }

  @GetMapping("{rapportId}/krav")
  fun getResultatPrKrav(
    @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatKravEkstern>> {
    val testgrunnlagLoeysing =
      eksternResultatDAO.findTestgrunnlagLoeysingFromRapportId((rapportId))
        ?: return ResponseEntity.badRequest().build()

    val testgrunnlag =
      testgrunnlagDAO.getTestgrunnlag(testgrunnlagLoeysing.testgrunnlagId).getOrElse {
        return ResponseEntity.badRequest().build()
      }

    val resultatKrav = resultatService.getResultatPrKrav(testgrunnlag.kontrollId, null, null, null)
    if (resultatKrav.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val resultatTemaEkstern = resultatKrav.map { it.toResultatKravEkstern() }

    return ResponseEntity.ok(resultatTemaEkstern)
  }

  @GetMapping("{rapportId}/{suksesskriterium}")
  fun getResultatListKontroll(
    @PathVariable rapportId: String,
    @PathVariable suksesskriterium: String
  ): ResponseEntity<List<TestresultatDetaljertEkstern>> {
    val testgrunnlagLoeysing =
      eksternResultatDAO.findTestgrunnlagLoeysingFromRapportId((rapportId))
        ?: return ResponseEntity.badRequest().build()

    if (!Regex("""^\d+\.\d+\.\d+$""").matches(suksesskriterium)) {
      return ResponseEntity.badRequest().build()
    }

    val krav =
      runCatching { kravregisterClient.getKrav(suksesskriterium) }
        .getOrElse {
          return ResponseEntity.badRequest().build()
        }

    val results =
      resultatService
        .getResultatListTestgrunnlag(
          testgrunnlagLoeysing.testgrunnlagId, testgrunnlagLoeysing.loeysingId, krav.id
        )
        .map { it.toTestresultatDetaljertEkstern() }

    return ResponseEntity.ok(results)
  }
}
