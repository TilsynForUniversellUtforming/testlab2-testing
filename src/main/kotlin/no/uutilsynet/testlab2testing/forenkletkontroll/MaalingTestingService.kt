package no.uutilsynet.testlab2testing.forenkletkontroll

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.testing.manuelltesting.AutotestingService
import no.uutilsynet.testlab2testing.testing.manuelltesting.TestKoeyring
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MaalingTestingService(
    val autotestingService: AutotestingService,
    val brukarService: BrukarService,
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testregelDAO: TestregelDAO,
    val maalingService: MaalingService
) {

  private val logger = LoggerFactory.getLogger(MaalingTestingService::class.java)

  suspend fun restartTesting(
      statusDTO: MaalingResource.StatusDTO,
      maaling: Maaling.TestingFerdig,
      brukar: Brukar
  ): ResponseEntity<Any> {
    return coroutineScope {
      logger.info("Restarter testing for måling ${maaling.id}")
      val loeysingIdList = maalingService.getValidatedLoeysingList(statusDTO)
      val testreglar = testreglarForMaaling(maaling.id)

      val (retestList, rest) =
          maaling.testKoeyringar.partition { loeysingIdList.contains(it.loeysing.id) }

      val testKoeyringar =
          autotestingService.startTesting(
              maaling.id, brukar, retestList.map { it.loeysing }, testreglar)

      saveUpdated(maaling, rest.plus(testKoeyringar)).getOrThrow()
      ResponseEntity.ok().build()
    }
  }

  private suspend fun saveUpdated(
      maaling: Maaling,
      testKoeyringar: List<TestKoeyring>
  ): Result<Maaling> {
    val updated =
        Maaling.Testing(
            id = maaling.id,
            navn = maaling.navn,
            datoStart = maaling.datoStart,
            testKoeyringar = testKoeyringar)
    return withContext(Dispatchers.IO) { maalingDAO.save(updated) }
  }

  suspend fun startTesting(maaling: Maaling.Kvalitetssikring, brukar: Brukar): ResponseEntity<Any> {
    return coroutineScope {
      val loeysingIdList =
          maaling.crawlResultat.filterIsInstance<CrawlResultat.Ferdig>().map { it.loeysing }
      val testreglar = testreglarForMaaling(maaling.id)

      val testKoeyringar =
          autotestingService.startTesting(maaling.id, brukar, loeysingIdList, testreglar)

      saveUpdated(maaling, testKoeyringar).getOrThrow()
      ResponseEntity.ok().build()
    }
  }

  private suspend fun testreglarForMaaling(maalingId: Int): List<Testregel> {
    val testreglar =
        withContext(Dispatchers.IO) { testregelDAO.getTestreglarForMaaling(maalingId) }
            .getOrElse {
              logger.error("Feila ved henting av actregler for måling $maalingId", it)
              throw it
            }
            .onEach { it.validateTestregel().getOrThrow() }
    return testreglar
  }
}
