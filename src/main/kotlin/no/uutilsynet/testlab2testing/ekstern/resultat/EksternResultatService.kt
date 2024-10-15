package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternResultatService(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val resultatService: ResultatService,
    @Autowired val kravregisterClient: KravregisterClient
) {

  @Transactional
  fun publiserResultat(kontrollId: Int) {
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest

    val loeysingList = testgrunnlag.sideutval.map { it.loeysingId }

    loeysingList.forEach { eksternResultatDAO.publiserResultat(testgrunnlag.id, it).getOrThrow() }
  }

  fun findTestForOrgNr(orgnr: String): Result<TestListElementEkstern> {

    val loeysingList = loeysingsRegisterClient.search(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.info("Fann ingen løysingar for verkemd med orgnr $orgnr")
      throw NoSuchElementException("Fann ingen løysingar for verkemd med orgnr $orgnr")
    }

    val loeysingIdList = loeysingList.map { it.id }

    val testList = eksternResultatDAO.getTestsForLoeysingIds(loeysingIdList)
    if (testList.isEmpty()) {
      logger.info("Fann ingen gyldige testar for orgnr $orgnr")
      throw NoSuchElementException("Fann ingen gyldige testar for orgnr $orgnr")
    }

    val verksemd = loeysingsRegisterClient.searchVerksemd(orgnr).getOrThrow().firstOrNull()

    val testEksternList =
        testList
            .flatMap { test ->
              val kontrollResult =
                  resultatService.getKontrollResultat(test.kontrollId).first { result ->
                    result.testType == TestgrunnlagType.OPPRINNELEG_TEST
                  }

              kontrollResult.loeysingar.map { loeysingResult ->
                test.toListElement(loeysingResult.loeysingNamn, loeysingResult.score)
              }
            }
            .sortedBy { it.publisert }

    return Result.success(
        TestListElementEkstern(
            verksemd =
                VerksemdEkstern(
                    namn = verksemd?.namn ?: orgnr,
                    organisasjonsnummer = verksemd?.organisasjonsnummer ?: orgnr,
                ),
            testList = testEksternList))
  }
}
