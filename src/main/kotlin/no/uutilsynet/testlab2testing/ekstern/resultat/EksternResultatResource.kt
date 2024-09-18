package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatDAO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
  @Autowired val eksternResultatDAO: EksternResultatDAO,
  @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
  @Autowired val resultatDAO: ResultatDAO
) {

  @GetMapping
  fun findTestForOrgNr(
    @RequestParam("orgnr") orgnr: String
  ): TestListElementEkstern? {
    logger.debug("Henter tester for orgnr $orgnr")

    val loeysingList = loeysingsRegisterClient.search(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.info("Fann ingen løsyingar med orgnr $orgnr")
      return null
    }

    val loeysingIdList = loeysingList.map { it.id }

    val testList = eksternResultatDAO.getTestsForLoeysingIds(loeysingIdList)
    if (testList.isEmpty()) {
      logger.info("Fann ingen gyldige testar for orgnr $orgnr")
      return null
    }

    val verksemd = loeysingsRegisterClient.searchVerksemd(orgnr)
      .getOrThrow()
      .firstOrNull()


    val testEksternList = testList.flatMap { test ->
      val relatedResults = resultatDAO.getResultatKontroll(test.kontrollId)
        .filter { resultat -> loeysingIdList.contains(resultat.loeysingId) }

      relatedResults.map { result ->
        test.toListElement(
          loeysingList.find { it.id == result.loeysingId }?.namn ?: "Ukjent",
          result.score.toInt()
        )
      }
    }.sortedBy { it.publisert }

    return TestListElementEkstern(
      verksemd = VerksemdEkstern(
        namn = verksemd?.namn ?: orgnr,
        organisasjonsnummer = verksemd?.organisasjonsnummer ?: orgnr,
      ),
      testList = testEksternList
    )
  }
}
