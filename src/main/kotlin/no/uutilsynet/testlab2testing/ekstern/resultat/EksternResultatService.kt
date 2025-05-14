package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import no.uutilsynet.testlab2testing.resultat.Resultat
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatService
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EksternResultatService(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val resultatService: ResultatService,
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val maalingDAO: MaalingDAO,
    @Autowired val kravregisterClient: KravregisterClient,
    private val testregelService: TestregelService
) {

  private val logger = LoggerFactory.getLogger(EksternResultatResource::class.java)

  fun findTestForOrgNr(org: String): Result<TestListElementEkstern> {
    val verksemd: VerksemdEkstern = getVerksemd(org)
    val testEksternList = getLoysingarForOrgnr(org).toListElementForLoeysingar().toTestEksternList()

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

  private fun getKontrollResult(test: TestListElementDB): Resultat {
    return resultatService
        .getKontrollResultatMedType(test.kontrollId, test.kontrollType)
        .filterResultatOnLoeysing(test)
  }

  private fun List<Resultat>.filterResultatOnLoeysing(test: TestListElementDB): Resultat {
    return this.first()
        .copy(loeysingar = filterLoeysingarOnId(this.first().loeysingar, test.loeysingId))
  }

  private fun filterLoeysingarOnId(loeysingar: List<LoeysingResultat>, loeysingId: Int) =
      loeysingar.filter { loeysing -> loeysing.loeysingId == loeysingId }

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
    val testList = this.map { it.id }.let { eksternResultatDAO.getTestsForLoeysingIds(it) }

    if (testList.isEmpty()) {
      logger.info("Fann ingen gyldige testar for orgnr ${this.first().orgnummer}")
      throw NoSuchElementException("Fann ingen gyldige testar for orgnr ${this.first().orgnummer}")
    }

    return testList
  }

  private fun getLoysingarForOrgnr(orgnr: String): List<Loeysing> {
    val loeysingList = loeysingsRegisterClient.searchLoeysingByVerksemd(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.info("Fann ingen løysingar for verkemd med orgnr $orgnr")
      throw NoSuchElementException("Fann ingen løysingar for verkemd med orgnr $orgnr")
    }
    return loeysingList
  }

  fun getRapportForLoeysing(
      rapportId: String,
      loeysingId: Int
  ): List<ResultatOversiktLoeysingEkstern> {
    return getKontrollLoeysing(rapportId, loeysingId)
        .mapCatching { getResultatEksternFromRapportLoeysing(it) }
        .getOrThrow()
  }

  private fun getKontrollLoeysing(
      rapportId: String,
      loeysingId: Int
  ): Result<KontrollIdLoeysingId> {
    return eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId)).map {
      filterkontrollIdLoeysingIdOnLoeysingId(it, loeysingId)
    }
  }

  private fun filterkontrollIdLoeysingIdOnLoeysingId(
      kontrollIdLoeysingIds: List<KontrollIdLoeysingId>,
      loeysingId: Int
  ): KontrollIdLoeysingId {
    return kontrollIdLoeysingIds.first { it.loeysingId == loeysingId }
  }

  private fun resultatOversiktLoeysing(
      kontrollLoeysing: KontrollIdLoeysingId
  ): List<ResultatOversiktLoeysing> {
    return resultatService.getKontrollLoeysingResultatIkkjeRetest(
        kontrollLoeysing.kontrollId, kontrollLoeysing.loeysingId)
  }

  fun getResultatEksternFromRapportLoeysing(kontrollLoeysing: KontrollIdLoeysingId) =
      resultatOversiktLoeysing(kontrollLoeysing).map { it.toResultatOversiktLoeysingEkstern() }

  fun getRapportPrTema(rapportId: String, loeysingId: Int): Result<List<ResultatTemaEkstern>> {
    return runCatching {
      getKontrollIdLoeysingIdsForRapportId(rapportId)
          .filter { it.loeysingId == loeysingId }
          .map { getResultatTemaList(it) }
          .flatten()
          .map { it.toResultatTemaEkstern() }
    }
  }

  fun getRapportPrKrav(rapportId: String, loeysingId: Int): Result<List<ResultatKravEkstern>> {
    return runCatching {
      getKontrollIdLoeysingIdsForRapportId(rapportId)
          .filter { it.loeysingId == loeysingId }
          .map { getResultatKravList(it) }
          .flatten()
          .map { it.toResultatKravEkstern() }
    }
  }

  private fun getResultatTemaList(kontrollLoeysing: KontrollIdLoeysingId) =
      resultatService.getResultatPrTema(
          kontrollLoeysing.kontrollId, null, kontrollLoeysing.loeysingId, null, null)

  private fun getResultatKravList(kontrollLoeysing: KontrollIdLoeysingId) =
      resultatService.getResultatPrKrav(
          kontrollLoeysing.kontrollId, null, kontrollLoeysing.loeysingId, null, null)

  private fun testresultatToDetaljertEkstern(
      kontrollLoeysing: KontrollIdLoeysingId,
      testregel: Testregel
  ) =
      getResultatPrTestregel(kontrollLoeysing, testregel).map {
        it.toTestresultatDetaljertEkstern(testregel)
      }

  private fun getTestregelFromTestregelId(testregelId: Int): Testregel {
    return testregelService.getTestregel(testregelId)
  }

  private fun getResultatPrTestregel(kontrollLoeysing: KontrollIdLoeysingId, testregel: Testregel) =
      resultatService.getResultatListKontroll(
          kontrollLoeysing.kontrollId, kontrollLoeysing.loeysingId, testregel.id)

  private fun getKontrollIdLoeysingIdsForRapportId(rapportId: String): List<KontrollIdLoeysingId> {
    return eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId)).getOrThrow()
  }

  fun getResultatListKontrollAsEksterntResultat(
      rapportId: String,
      loeysingId: Int,
      testregelId: Int
  ): List<TestresultatDetaljertEkstern> {
    return getKontrollLoeysing(rapportId, loeysingId)
        .mapCatching {
          val testregel = getTestregelFromTestregelId(testregelId)
          testresultatToDetaljertEkstern(it, testregel)
        }
        .getOrThrow()
  }
}
