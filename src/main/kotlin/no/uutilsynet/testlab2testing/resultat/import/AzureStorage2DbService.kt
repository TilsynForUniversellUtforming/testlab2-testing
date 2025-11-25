package no.uutilsynet.testlab2testing.resultat.import

import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalCache
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.AutotesterTestresultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatDBBase
import no.uutilsynet.testlab2testing.toSingleResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.stream.Collectors

@Service
class AzureStorage2DbService(
    private val testresultatDAO: TestresultatDAO,
    private val autoTesterClient: AutoTesterClient,
    private val maalingService: MaalingService,
    private val sideutvalDAO: SideutvalDAO,
    private val brukarService: BrukarService,
    private val testregelCache: TestregelCache,
    private val testkoeyringDAO: TestkoeyringDAO
) {

  val logger = LoggerFactory.getLogger(AzureStorage2DbService::class.java)

  fun getTestresultatFraAzureStorage(
      maalingId: Int,
      loeysingId: Int,
      resulttatType: AutoTesterClient.ResultatUrls = AutoTesterClient.ResultatUrls.urlBrot
  ): Result<List<AutotesterTestresultat>> {
    val testkoeyringMaalingLoeysing =
        maalingService.getFilteredAndFerdigTestkoeyringar(maalingId, loeysingId)

    logger.debug(
        "Get testresultat from Azure Storage for maalingId: $maalingId, loeysingId: $loeysingId, size: ${testkoeyringMaalingLoeysing.size}")
    return runBlocking {
        autoTesterClient
            .fetchResultat(testkoeyringMaalingLoeysing, resulttatType)
            .toSingleResult()
            .map { it.values.flatten() }
    }
  }

    fun getBrukarForTestKoeyring(maalingId: Int, loeysingId: Int): Int? {

      val brukarId = testkoeyringDAO.getBrukarIdForTestkoeyring(maalingId, loeysingId)
        return brukarId ?: 1

  }

  fun getTestresultat(maalingId: Int, loeysingId: Int): List<TestresultatDBBase> {
    val sideutvalCache = SideutvalCache(sideutvalDAO, maalingId, loeysingId)

      val brukarId = getBrukarForTestKoeyring(maalingId, loeysingId)

    val result =
        getTestresultatFraAzureStorage(maalingId, loeysingId)
            .getOrThrow()
            .map { it as TestResultat }
            .parallelStream()
            .map { mapAutotesterResultatToDbFormat(it, maalingId, sideutvalCache, brukarId) }
            .collect(Collectors.toList())

    logger.debug(
        "Get testresultat mapped to DB format for maalingId: $maalingId, loeysingId: $loeysingId, size: ${result.size}")
    return result
  }

  fun createTestresultatDB(maalingId: Int, loeysingId: Int): Result<List<Int>> {
    return runCatching {
      val testresultatList = getTestresultat(maalingId, loeysingId)
      testresultatList
          .parallelStream()
          .map { it ->
            /*if (index % 500 == 0) {*/
            logger.debug(
                "Creating testresultat in DB for loeysingId: $loeysingId,  testregelId: ${it.testregelId}, sideutvalId: ${it.sideutvalId}")
            /*}*/
            testresultatDAO.create(it)
          }
          .collect(Collectors.toList())
    }
  }

  fun mapAutotesterResultatToDbFormat(
      testresultat: TestResultat,
      maalingId: Int,
      sideutvalCache: SideutvalCache,
      brukarId: Int?
  ): TestresultatDBBase {

    /*if (index % 500 == 0) {*/

    logger.debug(
        "Mapping autotester resultat to DB format for loeysing: {}, testregelId: {}, side: {}",
        testresultat.loeysingId,
        testresultat.testregelId,
        testresultat.side)

    /*}*/

    return if (testresultat.elementResultat == TestresultatUtfall.brot) {

      TestresultatDBBase(
          null,
          maalingId = maalingId,
          loeysingId = testresultat.loeysingId,
          testregelId = testregelCache.getTestregelByKey(testresultat.testregelId).id,
          sideutvalId = sideutvalCache.getSideutvalId(testresultat.side),
          testUtfoert = testresultat.testVartUtfoert.atZone(ZoneId.systemDefault()).toInstant(),
          elementUtfall = testresultat.elementUtfall,
          elementResultat = testresultat.elementResultat,
          elementOmtalePointer = testresultat.elementOmtale?.pointer,
          elmentOmtaleHtml = testresultat.elementOmtale?.htmlCode,
          elementOmtaleDescription = testresultat.elementOmtale?.description,
          brukarId = brukarId ?: 1)
    } else {
        TestresultatDBBase(
            null,
            maalingId = maalingId,
            loeysingId = testresultat.loeysingId,
            testregelId = testregelCache.getTestregelByKey(testresultat.testregelId).id,
            sideutvalId = sideutvalCache.getSideutvalId(testresultat.side),
            testUtfoert = testresultat.testVartUtfoert.atZone(ZoneId.systemDefault()).toInstant(),
            elementUtfall = testresultat.elementUtfall,
            elementResultat = testresultat.elementResultat,
            elementOmtalePointer = null,
            elmentOmtaleHtml = null,
            elementOmtaleDescription = null,
            brukarId = brukarService.getUserId() ?: 0
        )
    }
  }
}