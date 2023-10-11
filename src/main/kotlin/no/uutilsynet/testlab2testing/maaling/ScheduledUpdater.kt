package no.uutilsynet.testlab2testing.maaling

import java.time.Instant
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledUpdater(
    val maalingDAO: MaalingDAO,
    val crawlerClient: CrawlerClient,
    val autoTesterClient: AutoTesterClient
) {

  private val logger = LoggerFactory.getLogger(ScheduledUpdater::class.java)

  @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
  fun scheduledUpdate() {
    updateStatuses()
  }

  private fun updateStatuses() {
    val alleMaalinger: List<Maaling> =
        maalingDAO.getMaalingListByStatus(listOf(MaalingStatus.crawling, MaalingStatus.testing))

    runCatching {
          val statusCrawling = alleMaalinger.filterIsInstance<Maaling.Crawling>()
          val oppdaterteMaalinger = statusCrawling.map { updateCrawlingStatuses(it) }
          maalingDAO.saveMany(oppdaterteMaalinger).getOrThrow()
          if (statusCrawling.isNotEmpty()) {
            logger.info(
                "oppdaterte status for ${statusCrawling.size} målinger med status `crawling`")
          }
        }
        .getOrElse {
          logger.error("klarte ikke å oppdatere status for målinger med status `crawling`", it)
        }

    runCatching {
          val statusTesting = alleMaalinger.filterIsInstance<Maaling.Testing>()
          val oppdaterteMaalinger = statusTesting.map { updateTestingStatuses(it) }
          maalingDAO.saveMany(oppdaterteMaalinger).getOrThrow()
          if (statusTesting.isNotEmpty()) {
            logger.info("oppdaterte status for ${statusTesting.size} målinger med status `testing`")
          }
        }
        .getOrElse {
          logger.error(
              "klarte ikke å oppdatere status for målinger med status `testing` i maalingDAO", it)
        }
  }

  fun updateCrawlingStatuses(maaling: Maaling.Crawling): Maaling {
    val oppdaterteResultater =
        maaling.crawlResultat.map {
          when (it) {
            is CrawlResultat.Starta -> {
              updateCrawlingStatus(it) { cr ->
                crawlerClient.getStatus(cr as CrawlResultat.Starta, maaling.id)
              }
            }
            is CrawlResultat.IkkjeStarta -> {
              updateCrawlingStatus(it) { cr ->
                crawlerClient.getStatus(cr as CrawlResultat.IkkjeStarta, maaling.id)
              }
            }
            else -> it
          }
        }
    val oppdatertMaaling = maaling.copy(crawlResultat = oppdaterteResultater)
    return Maaling.toKvalitetssikring(oppdatertMaaling) ?: oppdatertMaaling
  }

  fun updateTestingStatuses(maaling: Maaling.Testing): Maaling {
    val oppdaterteTestKoeyringar =
        maaling.testKoeyringar.map {
          when (it) {
            is TestKoeyring.Starta -> {
              updateTestingStatus(it) { testKoeyring ->
                autoTesterClient.updateStatus(testKoeyring as TestKoeyring.Starta)
              }
            }
            is TestKoeyring.IkkjeStarta -> {
              updateTestingStatus(it) { testKoeyring ->
                autoTesterClient.updateStatus(testKoeyring as TestKoeyring.IkkjeStarta)
              }
            }
            else -> {
              it
            }
          }
        }
    val oppdatertMaaling = maaling.copy(testKoeyringar = oppdaterteTestKoeyringar)
    return Maaling.toTestingFerdig(oppdatertMaaling) ?: oppdatertMaaling
  }

  companion object {
    private val logger = LoggerFactory.getLogger(ScheduledUpdater::class.java)

    private val failedCrawlStatusAttempts = mutableMapOf<CrawlResultat, Int>()

    fun updateCrawlingStatus(
        crawlResultat: CrawlResultat,
        getNewStatus: (CrawlResultat) -> Result<CrawlStatus>
    ): CrawlResultat {
      val updated =
          getNewStatus(crawlResultat).map { newStatus -> updateStatus(crawlResultat, newStatus) }
      return if (updated.isFailure) {
        val previousAttempts = failedCrawlStatusAttempts.getOrDefault(crawlResultat, 0)
        if (previousAttempts < 12) {
          failedCrawlStatusAttempts[crawlResultat] = previousAttempts + 1
          crawlResultat
        } else {
          logger.error(
              "feila da eg forsøkte å oppdatere status for løysing ${crawlResultat.loeysing.id}",
              updated.exceptionOrNull())
          CrawlResultat.Feila(
              "Crawling av ${crawlResultat.loeysing.url} feila. Eg klarte ikkje å hente status frå crawleren.",
              crawlResultat.loeysing,
              Instant.now())
        }
      } else {
        failedCrawlStatusAttempts.remove(crawlResultat)
        updated.getOrThrow()
      }
    }

    private val failedTestingStatusAttempts = mutableMapOf<TestKoeyring, Int>()

    fun updateTestingStatus(
        testKoeyring: TestKoeyring,
        getNewStatus: (TestKoeyring) -> Result<AutoTesterClient.AutoTesterStatus>
    ): TestKoeyring {
      val updated =
          getNewStatus(testKoeyring).map { newStatus ->
            TestKoeyring.updateStatus(testKoeyring, newStatus)
          }
      return if (updated.isFailure) {
        val previousAttempts = failedTestingStatusAttempts.getOrDefault(testKoeyring, 0)
        if (previousAttempts < 12) {
          failedTestingStatusAttempts[testKoeyring] = previousAttempts + 1
          testKoeyring
        } else {
          logger.error(
              "feila da eg forsøkte å oppdatere status for løysing ${testKoeyring.loeysing.id}",
              updated.exceptionOrNull())
          TestKoeyring.Feila(
              testKoeyring.crawlResultat,
              Instant.now(),
              "Testing av ${testKoeyring.loeysing.url} feila. Eg klarte ikkje å hente status frå autotestaren.")
        }
      } else {
        failedTestingStatusAttempts.remove(testKoeyring)
        updated.getOrThrow()
      }
    }
  }
}
