package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val resultatService: ResultatService
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
            testList = testEksternList))
  }
}
