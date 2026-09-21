package no.uutilsynet.testlab2testing.forenkletkontroll

import io.micrometer.observation.annotation.Observed
import jakarta.validation.ClockProvider
import java.time.Instant
import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.forenkletkontroll.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.utval.Utval
import no.uutilsynet.testlab2testing.loeysing.utval.UtvalDAO
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.AutotesterTestresultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDTO
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("LongParameterList")
@Service
class MaalingService(
    val maalingDAO: MaalingDAO,
    val maalingReadService: MaalingReadService,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val utvalDAO: UtvalDAO,
    val autoTesterClient: AutoTesterClient,
    val testreglClient: TestregelClient,
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
    when {
      utvalId != null ->
          maalingDAO.createMaaling(
              navn, localDateNorway, getUtval(utvalId), testregelIdList, crawlParameters)
      loeysingIdList != null ->
          maalingDAO.createMaaling(
              navn, localDateNorway, loeysingIdList, testregelIdList, crawlParameters)
      else -> error("utvalId eller loeysingIdList må vere gitt")
    }
  }

  fun updateMaaling(kontroll: Kontroll): Result<Unit> = runCatching {
    val maalingId = maalingReadService.getMaalingIdFromKontrollId(kontroll.id)
    require(maalingId != null) { "Måling finns ikkje for kontroll" }
    val maalingEdit = kontroll.toMaalingEdit(maalingId)
    maalingDAO.updateMaaling(maalingEdit.toMaaling())
  }

  fun updateMaaling(dto: EditMaalingDTO): Result<Unit> = runCatching {
    val maalingCopy = dto.toMaaling()
    maalingDAO.updateMaaling(maalingCopy)
  }

  fun deleteKontrollMaaling(kontrollId: Int): Result<Unit> = runCatching {
    val maalingId = maalingReadService.getMaalingIdFromKontrollId(kontrollId)
    if (maalingId != null) {
      return deleteMaaling(maalingId)
    }
  }

  fun deleteMaaling(id: Int): Result<Unit> = runCatching { maalingDAO.deleteMaaling(id) }

  fun isMaalingFerdigTestet(maalingId: Int): Boolean {
    return maalingReadService.isMaalingFerdigTesta(maalingId)
  }

  private fun validatedTestregeldList(dto: MaalingResource.NyMaalingDTO): List<Int> {
    return validatedTestregelList(dto.testregelIdList)
  }

  private fun validatedTestregelList(testregelIdList: List<Int>): List<Int> {
    return validateIdList(
            testregelIdList,
            testreglClient.getTestregelList().getOrThrow().map { it.id },
            "testregelIdList")
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
    return utvalDAO
        .getUtval(utvalId)
        .mapCatching {
          val loeysingar = loeysingsRegisterClient.getMany(it.loeysingar).getOrThrow()
          Utval(it.id, it.namn, loeysingar, it.oppretta)
        }
        .getOrThrow()
  }

  private fun EditMaalingDTO.toMaaling(): Maaling {
    val navn = validateNamn(this.navn).getOrThrow()

    return when (val maaling = maalingDAO.getMaaling(this.id)) {
      is Maaling.Planlegging -> {
        this.crawlParameters?.validateParameters()
        val loeysingList = getLoeysingarForMaaling(this.loeysingIdList, maaling.id)
        val testregelList = getTestreglarForMaaling(this.testregelIdList, maaling.id)

        maaling.copy(
            navn = navn,
            loeysingList = loeysingList,
            testregelList = testregelList.map { it.toTestregelBase() },
            crawlParameters = this.crawlParameters ?: maaling.crawlParameters)
      }
      is Maaling.Crawling -> maaling.copy(navn = navn)
      is Maaling.Testing -> maaling.copy(navn = navn)
      is Maaling.TestingFerdig -> maaling.copy(navn = navn)
      is Maaling.Kvalitetssikring -> maaling.copy(navn = navn)
    }
  }

  private fun getTestreglarForMaaling(
      testregelIdList: List<Int>?,
      maalingId: Int
  ): List<Testregel> {
    return testregelIdList?.let { idList ->
      testreglClient.getTestregelList().getOrThrow().filter { idList.contains(it.id) }
    }
        ?: emptyList<Testregel>().also { logger.warn("Måling $maalingId har ikkje testreglar") }
  }

  private fun getLoeysingarForMaaling(
      idList: List<Int>?,
      maalingId: Int,
  ): List<Loeysing> {
    return idList?.let { idList -> loeysingsRegisterClient.getMany(idList) }?.getOrThrow()
        ?: emptyList<Loeysing>().also { logger.warn("Måling $maalingId har ikkje løysingar") }
  }

  fun getFerdigeTestkoeyringar(maalingId: Int): List<TestkoeyringDTO.Ferdig> {
    return getTestkoeyringar(maalingId).filterIsInstance<TestkoeyringDTO.Ferdig>()
  }

  private suspend fun mapTestkoeyringToTestresultatBrot(
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

  fun getValidatedLoeysingList(statusDTO: MaalingResource.StatusDTO, id: Int): List<Int> {
    return validateIdList(statusDTO.loeysingIdList, getValidIds(statusDTO, id), "loeysingIdList")
        .getOrThrow()
  }

  private fun getValidIds(statusDTO: MaalingResource.StatusDTO, maalingId: Int): List<Int> {
    return if (statusDTO.loeysingIdList?.isNotEmpty() == true) {
      getLoeysingarForMaaling(statusDTO.loeysingIdList, maalingId).map { it.id }
    } else {
      emptyList()
    }
  }

  fun getTestreglarForMaaling(maalingId: Int): Result<List<Testregel>> {
    return runCatching {
      val testregelIds = maalingReadService.getTestregelIdsForMaaling(maalingId)
      testreglClient.getTestregelListFromIds(testregelIds).getOrThrow()
    }
  }

  fun getLoeysingarForMaaling(id: Int): List<Loeysing> =
      maalingReadService.getLoeysingarForMaaling(id)

  @Observed(name = "MaalingService.getMaalingForKontroll")
  fun getMaalingForKontroll(kontrollId: Int): Int {
    return maalingReadService.getMaalingIdFromKontrollId(kontrollId)
        ?: throw NoSuchElementException("Fant ikkje måling for kontrollId $kontrollId")
  }

  fun getKontrollIdForMaaling(maalingId: Int): Int {
    return maalingReadService.getKontrollIdFromMaalingId(maalingId)
  }

  fun getTestkoeyringar(maalingId: Int): List<TestkoeyringDTO> {
    return testkoeyringDAO.getTestkoeyringarForMaaling(maalingId)
  }
}
