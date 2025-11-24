package no.uutilsynet.testlab2testing.ekstern.resultat

import io.micrometer.observation.annotation.Observed
import java.util.stream.Collectors
import no.uutilsynet.testlab2testing.ekstern.resultat.model.*
import no.uutilsynet.testlab2testing.ekstern.resultat.paginering.TestresultatEksternAssembler
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Verksemd
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import no.uutilsynet.testlab2testing.resultat.Resultat
import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatService
import no.uutilsynet.testlab2testing.testregel.TestregelService
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.stereotype.Service

@Service
class EksternResultatService(
    private val eksternResultatDAO: EksternResultatDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val resultatService: ResultatService,
    private val testregelService: TestregelService,
    private val logMessages: LogMessages,
    private val testresultatEksternAssembler: TestresultatEksternAssembler,
    private val pagedResourcesAssembler: PagedResourcesAssembler<TestresultatDetaljertEkstern>
) {

  private val logger = LoggerFactory.getLogger(EksternResultatService::class.java)

  fun findTestForOrgNr(org: String): Result<TestListElementEkstern> {
    val verksemd: VerksemdEkstern = getVerksemd(org).getOrThrow()
    val testEksternList = getLoysingarForOrgnr(org).toListElementForLoeysingar().toTestEksternList()

    return Result.success(TestListElementEkstern(verksemd = verksemd, testList = testEksternList))
  }

  fun toTestListEkstern(test: TestListElementDB): List<TestEkstern> {
    return getKontrollResult(test).loeysingar.map { loeysingResult ->
      test.toListElement(loeysingResult, loeysingResult.score)
    }
  }

  private fun List<TestListElementDB>.toTestEksternList(): List<TestEkstern> {
    return this.sortedBy { it.publisert }.map(::toTestListEkstern).flatten()
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

  private fun getVerksemd(orgnr: String): Result<VerksemdEkstern> {
    return searchForOrganization(orgnr).mapCatching { verksemder ->
      mapToVerksemdEkstern(verksemder)
          .ifEmpty { listOf(VerksemdEkstern(orgnr, orgnr)) }
          .singleWithMessage(orgnr)
    }
  }

  private fun List<VerksemdEkstern>.singleWithMessage(orgnr: String): VerksemdEkstern {
    return runCatching { this.single() }
        .fold(
            onSuccess = { it },
            onFailure = {
              throw IllegalArgumentException(logMessages.verksemdMultipleFoundForSearch(orgnr))
            })
  }

  private fun searchForOrganization(orgnr: String) = loeysingsRegisterClient.searchVerksemd(orgnr)

  private fun mapToVerksemdEkstern(
      verksemder: List<Verksemd>,
  ): List<VerksemdEkstern> {
    return verksemder.map { VerksemdEkstern(it.namn, it.organisasjonsnummer) }
  }

  private fun List<Loeysing>.toListElementForLoeysingar(): List<TestListElementDB> {
    val testList = getTestarForLoeysing(this.map { it.id })

    if (testList.isEmpty()) {
      logger.warn(logMessages.teststNotFoundForOrgnr(this.first().orgnummer))
      throw NoSuchElementException(logMessages.teststNotFoundForOrgnr(this.first().orgnummer))
    }

    return testList
  }

  fun getTestarForLoeysing(loeysingIds: List<Int>): List<TestListElementDB> {
    return eksternResultatDAO.getTestsForLoeysingIds(loeysingIds)
  }

  fun getTestEksternForRapportLoeysing(rapportId: String, loeysingId: Int): TestEkstern {
    val kontroll = getKontrollLoeysing(rapportId, loeysingId).getOrThrow()
    val testList =
        getTestarForLoeysing(listOf(loeysingId)).filter { it.kontrollId == kontroll.kontrollId }
    return testList.toTestEksternList().first()
  }

  private fun getLoysingarForOrgnr(orgnr: String): List<Loeysing> {
    val loeysingList = loeysingsRegisterClient.searchLoeysingByVerksemd(orgnr).getOrThrow()
    if (loeysingList.isEmpty()) {
      logger.warn(logMessages.loeysingNotFoundForOrgnr(orgnr))
      throw NoSuchElementException(logMessages.loeysingNotFoundForOrgnr(orgnr))
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
      testregel: Testregel,
      size: Int,
      pageNumber: Int
  ) =
      getResultatPrTestregel(kontrollLoeysing, testregel, size, pageNumber)
          .parallelStream()
          .map { it.toTestresultatDetaljertEkstern(testregel) }
          .collect(Collectors.toList())

  private fun getTestregelFromTestregelId(testregelId: Int): Testregel {
    return testregelService.getTestregel(testregelId)
  }

  private fun getResultatPrTestregel(
      kontrollLoeysing: KontrollIdLoeysingId,
      testregel: Testregel,
      size: Int,
      pageNumber: Int
  ) =
      resultatService.getResultatListKontroll(
          kontrollLoeysing.kontrollId, kontrollLoeysing.loeysingId, testregel.id, size, pageNumber)

  private fun getKontrollIdLoeysingIdsForRapportId(rapportId: String): List<KontrollIdLoeysingId> {
    return eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId)).getOrThrow()
  }

  fun getResultatListKontrollAsEksterntResultat(
      rapportId: String,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int
  ): List<TestresultatDetaljertEkstern> {
    return getKontrollLoeysing(rapportId, loeysingId)
        .mapCatching {
          val testregel = getTestregelFromTestregelId(testregelId)
          testresultatToDetaljertEkstern(it, testregel, size, pageNumber)
        }
        .getOrThrow()
  }

  fun getDetaljerResultatPaged(
      rapportId: String,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int
  ): CollectionModel<TestresultatDetaljertEkstern> {
    val pageRequest = PageRequest.of(pageNumber, size)
    val results =
        getResultatListKontrollAsEksterntResultat(
            rapportId, loeysingId, testregelId, size, pageNumber)

    val total =
        resultatService
            .getTalBrotForKontrollLoeysingTestregel(rapportId, loeysingId, testregelId)
            .getOrThrow()

    val page = PageImpl(results, pageRequest, total.toLong())
    return pagedResourcesAssembler.toModel(page, testresultatEksternAssembler)
  }

  @Observed(
      name = "ekstern_get_resultat_for_rapport",
      contextualName = "EksternResultatService.getResultatForRapport")
  fun getResultatForRapport(rapportId: String): List<TestEkstern> {

    return eksternResultatDAO.getTestsForRapportIds(rapportId).toTestEksternList().sortedBy {
      it.organisasjonsnamn
    }
  }

  fun eksporterRapportForLoeysing(rapportId: String, loeysingId: Int): List<TestresultatDetaljert> {
    return getKontrollLoeysing(rapportId, loeysingId)
        .mapCatching { resultatService.getBrotForRapportLoeysing(it.kontrollId, loeysingId) }
        .getOrThrow()
  }
}
