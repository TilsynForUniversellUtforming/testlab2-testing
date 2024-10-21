package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.Resultat
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EksternResultatService(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val resultatService: ResultatService,
    @Autowired val kontrollDAO: KontrollDAO
) {

  @Transactional
  fun publiserResultat(kontrollId: Int) {
    runCatching {
          val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
          logger.info("Publiserer resultat for kontroll med id $kontrollId")
          getLoeysingarForKontroll(kontroll).forEach {
            publiserResultatForLoeysingForKontroll(kontrollId, it.id)
          }
        }
        .onFailure {
          logger.error("Feil ved publisering av resultat", it)
          throw it
        }
  }

  @Transactional
  fun avpubliserResultat(kontrollId: Int) {
    runCatching {
          val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
          logger.info("Avpubliserer resultat for kontroll med id $kontrollId")
          getLoeysingarForKontroll(kontroll).forEach {
            eksternResultatDAO.avpubliserResultat(kontrollId, it.id).getOrThrow()
          }
        }
        .onFailure {
          logger.error("Feil ved avpublisering av resultat", it)
          throw it
        }
  }

  fun getLoeysingarForKontroll(
      kontroll: KontrollDAO.KontrollDB
  ): List<KontrollDAO.KontrollDB.Loeysing> {
    require(!(kontroll.utval == null || kontroll.utval.loeysingar.isEmpty())) {
      "Kontrollen har ingen loeysingar"
    }
    return kontroll.utval.loeysingar
  }

  private fun publiserResultatForLoeysingForKontroll(kontrollId: Int, it: Int) {
    eksternResultatDAO.publiserResultat(kontrollId, it).getOrThrow()
  }

  fun findTestForOrgNr(orgnr: String): Result<TestListElementEkstern> {
    val verksemd: VerksemdEkstern = getVerksemd(orgnr)
    val testEksternList =
        getLoysingarForOrgnr(orgnr).toListElementForLoeysingar().toTestEksternList()

    return Result.success(TestListElementEkstern(verksemd = verksemd, testList = testEksternList))
  }

  fun toTestListEkstern(test: TestListElementDB): List<TestEkstern> {
    return getKontrollResult(test).loeysingar.map { loeysingResult ->
      test.toListElement(loeysingResult.loeysingNamn, loeysingResult.score)
    }
  }

  private fun List<TestListElementDB>.toTestEksternList(): List<TestEkstern> {
    return this.map(::toTestListEkstern).flatten().sortedBy { it.publisert }
  }

  private fun getKontrollResult(test: TestListElementDB): Resultat =
      resultatService.getKontrollResultat(test.kontrollId).first { result ->
        result.testType == TestgrunnlagType.OPPRINNELEG_TEST
      }

  private fun getVerksemd(orgnr: String): VerksemdEkstern {
    return runCatching {
          loeysingsRegisterClient.searchVerksemd(orgnr).getOrThrow().map {
            VerksemdEkstern(it.namn, it.organisasjonsnummer)
          }
        }
        .getOrDefault(listOf(VerksemdEkstern(orgnr, orgnr)))
        .first()
  }

  private fun List<Loeysing>.toListElementForLoeysingar(): List<TestListElementDB> {
    val testList =
        this.map { it.id }.map { eksternResultatDAO.getTestsForLoeysingIds(listOf(it)) }.flatten()

    if (testList.isEmpty()) {
      logger.info("Fann ingen gyldige testar for orgnr ${this.first().orgnummer}")
      throw NoSuchElementException("Fann ingen gyldige testar for orgnr ${this.first().orgnummer}")
    }

    return testList
  }

  private fun getLoysingarForOrgnr(orgnr: String): List<Loeysing> {
    val loeysingList = loeysingsRegisterClient.search(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.info("Fann ingen løysingar for verkemd med orgnr $orgnr")
      throw NoSuchElementException("Fann ingen løysingar for verkemd med orgnr $orgnr")
    }
    return loeysingList
  }

  @CacheEvict("resultatKontroll")
  fun erKontrollPublisert(kontrollId: Int): Boolean {
    return eksternResultatDAO.erKontrollPublisert(kontrollId)
  }
}
