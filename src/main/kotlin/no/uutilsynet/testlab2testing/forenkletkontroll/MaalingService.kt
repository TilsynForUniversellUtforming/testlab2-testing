package no.uutilsynet.testlab2testing.forenkletkontroll

import io.micrometer.observation.annotation.Observed
import jakarta.validation.ClockProvider
import java.time.Instant
import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.testing.automatisk.*
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelService
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MaalingService(
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val utvalDAO: UtvalDAO,
    val aggregeringService: AggregeringService,
    val autoTesterClient: AutoTesterClient,
    val testregelService: TestregelService,
    val testkoeyringDAO: TestkoeyringDAO,
    val clockProvider: ClockProvider,
) {

  private val logger = LoggerFactory.getLogger(MaalingService::class.java)

  fun nyMaaling(kontrollId: Int, opprettKontroll: KontrollResource.OpprettKontroll) = runCatching {
    val dto = opprettKontroll.toNyMaaling()
    val navn = validateNamn(dto.navn).getOrThrow()
    val crawlParameters = dto.crawlParameters ?: CrawlParameters()
    crawlParameters.validateParameters()

    val localDateNorway = Instant.now(clockProvider.clock)

    val maalingId =
        maalingDAO.createMaaling(navn, localDateNorway, emptyList(), emptyList(), crawlParameters)
    maalingDAO.updateKontrollId(
        kontrollId,
        maalingId,
    )
  }

  fun nyMaaling(dto: MaalingResource.NyMaalingDTO): Result<Int> = runCatching {
    val navn = validateNamn(dto.navn).getOrThrow()
    val loeysingIdList = validateLoeyingsIdList(dto)
    val utvalId = validatedUtvalId(dto)
    val testregelIdList = validatedTestregeldList(dto)
    val crawlParameters = dto.crawlParameters ?: CrawlParameters()
    crawlParameters.validateParameters()

    val localDateNorway = Instant.now(clockProvider.clock)

    if (utvalId != null) {
      val utval = getUtval(utvalId)
      maalingDAO.createMaaling(navn, localDateNorway, utval, testregelIdList, crawlParameters)
    } else if (loeysingIdList != null) {
      maalingDAO.createMaaling(
          navn, localDateNorway, loeysingIdList, testregelIdList, crawlParameters)
    } else {
      throw IllegalArgumentException("utvalId eller loeysingIdList må være gitt")
    }
  }

  fun updateMaaling(kontroll: Kontroll): Result<Unit> = runCatching {
    val maalingId = maalingDAO.getMaalingIdFromKontrollId(kontroll.id)
    if (maalingId != null) {
      val maalingEdit = kontroll.toMaalingEdit(maalingId)
      maalingDAO.updateMaaling(maalingEdit.toMaaling())
    } else {
      throw IllegalArgumentException("Måling finns ikkje for kontroll")
    }
  }

  fun updateMaaling(dto: EditMaalingDTO): Result<Unit> = runCatching {
    val maalingCopy = dto.toMaaling()
    maalingDAO.updateMaaling(maalingCopy)
  }

  fun deleteKontrollMaaling(kontrollId: Int): Result<Unit> = runCatching {
    val maalingId = maalingDAO.getMaalingIdFromKontrollId(kontrollId)
    if (maalingId != null) {
      return deleteMaaling(maalingId)
    }
  }

  fun deleteMaaling(id: Int): Result<Unit> = runCatching { maalingDAO.deleteMaaling(id) }

  private fun validatedTestregeldList(dto: MaalingResource.NyMaalingDTO): List<Int> {
    return validatedTestregelList(dto.testregelIdList)
  }

  private fun validatedTestregelList(testregelIdList: List<Int>): List<Int> {
    return validateIdList(
            testregelIdList, testregelService.getTestregelList().map { it.id }, "testregelIdList")
        .getOrThrow()
  }

  private fun validatedUtvalId(dto: MaalingResource.NyMaalingDTO): Int? {
    val utvalIdList = utvalDAO.getUtvalList().getOrDefault(emptyList()).map { it.id }
    val utvalId =
        dto.utvalId?.let { validateIdList(listOf(it), utvalIdList, "utvalId").getOrThrow().first() }
    return utvalId
  }

  private fun validateLoeyingsIdList(dto: MaalingResource.NyMaalingDTO): List<Int>? {
    val loeysingIdList =
        dto.loeysingIdList?.let {
          val loeysingar =
              loeysingsRegisterClient.getMany(it).getOrThrow().map { loeysing -> loeysing.id }
          validateIdList(dto.loeysingIdList, loeysingar, "loeysingIdList").getOrThrow()
        }
    return loeysingIdList
  }

  private fun getUtval(utvalId: Int): Utval {
    val utval =
        utvalDAO
            .getUtval(utvalId)
            .mapCatching {
              val loeysingar = loeysingsRegisterClient.getMany(it.loeysingar).getOrThrow()
              Utval(it.id, it.namn, loeysingar, it.oppretta)
            }
            .getOrThrow()
    return utval
  }

  private fun EditMaalingDTO.toMaaling(): Maaling {
    val navn = validateNamn(this.navn).getOrThrow()
    this.crawlParameters?.validateParameters()


      return when (val maaling = maalingDAO.getMaaling(this.id)) {
      is Maaling.Planlegging -> {
          val loeysingList =
              this.loeysingIdList
                  ?.let { idList ->
                      loeysingsRegisterClient.getMany(idList) }
                  ?.getOrThrow()
                  ?: emptyList<Loeysing>().also {
                      logger.warn("Måling ${maaling.id} har ikkje løysingar")
                  }

          val testregelList =
              this.testregelIdList?.let { idList ->
                  testregelService.getTestregelList().filter { idList.contains(it.id) }
              }
                  ?: emptyList<Testregel>().also {
                      logger.warn("Måling ${maaling.id} har ikkje testreglar")
                  }

        maaling.copy(
            navn = navn,
            loeysingList = loeysingList,
            testregelList = testregelList.map { it.toTestregelBase() },
            crawlParameters = this.crawlParameters ?: maaling.crawlParameters)
      }
      is Maaling.Crawling -> maaling.copy(navn = this.navn)
      is Maaling.Testing -> maaling.copy(navn = this.navn)
      is Maaling.TestingFerdig -> maaling.copy(navn = this.navn)
      is Maaling.Kvalitetssikring -> maaling.copy(navn = this.navn)
    }
  }

    private fun getLoeysingarForMaaling(
      idList: List<Int>,
      maalingId: Int,
  ): List<Loeysing> {
    return loeysingsRegisterClient
        .getMany(idList)
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error("Feil ved henting av løysingar for måling $maalingId")
              throw it
            })
  }

  fun reimportAggregeringar(maalingId: Int, loeysingId: Int?) {
    runCatching {
      val maaling = maalingDAO.getMaaling(maalingId)
      require(maaling is Maaling.TestingFerdig) { "Måling er ikkje ferdig testa" }

      maaling.testKoeyringar
          .filterIsInstance<TestKoeyring.Ferdig>()
          .filter { filterTestkoeyring(it, loeysingId) }
          .forEach { aggregeringService.saveAggregering(it) }
    }
  }

  fun filterTestkoeyring(testKoeyring: TestKoeyring, loeysingId: Int?): Boolean {
    if (loeysingId != null) {
      return testKoeyring.loeysing.id == loeysingId
    }
    return true
  }

  fun getFerdigeTestkoeyringar(maalingId: Int): List<TestkoeyringDTO.Ferdig> {
    return testkoeyringDAO
        .getTestkoeyringarForMaaling(maalingId)
        .filterIsInstance<TestkoeyringDTO.Ferdig>()
  }

  suspend fun mapTestkoeyringToTestresultatBrot(
      ferdigeTestKoeyringar: List<TestkoeyringDTO.Ferdig>
  ) = autoTesterClient.fetchResultat(ferdigeTestKoeyringar, AutoTesterClient.ResultatUrls.urlBrot)

  @Observed(name = "MaalingService.getTestresultatMaalingLoeysing")
  fun getTestresultatMaalingLoeysing(
      maalingId: Int,
      loeysingId: Int?
  ): Result<List<AutotesterTestresultat>> {
    return runBlocking {
      mapTestkoeyringToTestresultatBrot(getFilteredAndFerdigTestkoeyringar(maalingId, loeysingId))
          .toSingleResult()
          .map { it.values.flatten() }
    }
  }

  @Observed(name = "MaalingService.getFilteredAndFerdigTestkoeyringar")
  fun getFilteredAndFerdigTestkoeyringar(maalingId: Int, loeysingId: Int?) =
      getFerdigeTestkoeyringar(maalingId).filter {
        loeysingId == null || it.loeysingId == loeysingId
      }

  fun hentEllerGenererAggregeringPrSide(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "side")) {
      val testKoeyringar = getTestKoeyringar(maalingId)
      testKoeyringar.forEach { aggregeringService.saveAggregeringSideAutomatisk(it) }
    }
    return aggregeringService.getAggregertResultatSide(maalingId).let { ResponseEntity.ok(it) }
  }

  fun hentEllerGenererAggregeringPrSuksesskriterium(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "suksesskriterium")) {
      val testKoeyringar = getTestKoeyringar(maalingId)
      testKoeyringar.forEach {
        aggregeringService.saveAggregertResultatSuksesskriteriumAutomatisk(it)
      }
    }
    return aggregeringService.getAggregertResultatSuksesskriterium(maalingId).let {
      ResponseEntity.ok(it)
    }
  }

  fun hentEllerGenererAggregeringPrTestregel(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "testresultat")) {
      logger.info("Aggregering er ikkje generert for måling $maalingId, genererer no")
      val testKoeyringar = getTestKoeyringar(maalingId)
      testKoeyringar.forEach { aggregeringService.saveAggregertResultatTestregelAutomatisk(it) }
    }
    return aggregeringService.getAggregertResultatTestregel(maalingId).let { ResponseEntity.ok(it) }
  }

  private fun getTestKoeyringar(maalingId: Int): List<TestKoeyring.Ferdig> {
    return runCatching {
          maalingDAO.getMaaling(maalingId).let { maaling ->
            Maaling.findFerdigeTestKoeyringar(maaling)
          }
        }
        .getOrElse {
          logger.error("Feila ved henting av testkøyringar for måling $maalingId", it)
          throw it
        }
  }

  fun getValidatedLoeysingList(statusDTO: MaalingResource.StatusDTO, id: Int): List<Int> {
    val validIds = getValidIds(statusDTO, id)
    val loeysingIdList =
        validateIdList(statusDTO.loeysingIdList, validIds, "loeysingIdList").getOrThrow()
    return loeysingIdList
  }

  private fun getValidIds(statusDTO: MaalingResource.StatusDTO, maalingId: Int): List<Int> {
    val validIds =
        if (statusDTO.loeysingIdList?.isNotEmpty() == true) {
          getLoeysingarForMaaling(statusDTO.loeysingIdList, maalingId).map { it.id }
        } else {
          emptyList()
        }
    return validIds
  }

  fun getTestreglarForMaaling(maalingId: Int): Result<List<Testregel>> {
    return runCatching {
      val testregelIds = maalingDAO.getTestrelIdForMaaling(maalingId)
      testregelService.getTestregeListFromIds(testregelIds)
    }
  }

  fun getMaalingForTestregel(testregelId: Int): List<Int> {
    return maalingDAO.getMaalingIdForTestregel(testregelId).getOrDefault(emptyList())
  }

  fun getMaalingList(maalingIds: List<Int>): List<MaalingListElement> {
    return maalingDAO.getMaalingList().filter { maalingIds.contains(it.id) }
  }

  @Observed(name = "MaalingService.getMaalingForKontroll")
  fun getMaalingForKontroll(kontrollId: Int): Int {
    return maalingDAO.getMaalingIdFromKontrollId(kontrollId)
        ?: throw NoSuchElementException("Fant ikkje måling for kontrollId $kontrollId")
  }
}
