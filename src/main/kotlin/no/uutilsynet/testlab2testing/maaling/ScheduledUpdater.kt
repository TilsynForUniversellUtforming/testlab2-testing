package no.uutilsynet.testlab2testing.maaling

import java.util.concurrent.TimeUnit
import no.uutilsynet.testlab2testing.toSingleResult
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
  fun updateStatuses() {
    val alleMaalinger: List<Maaling> =
        maalingDAO.getMaalingListByStatus(listOf(MaalingStatus.crawling, MaalingStatus.testing))

    runCatching {
          val statusCrawling = alleMaalinger.filterIsInstance<Maaling.Crawling>()
          val oppdaterteMaalinger =
              statusCrawling.map { updateCrawlingStatuses(it) }.toSingleResult().getOrThrow()
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
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
          oppdaterteMaalinger.map { maalingDAO.save(it) }.toSingleResult().getOrThrow()
          if (statusTesting.isNotEmpty()) {
            logger.info("oppdaterte status for ${statusTesting.size} målinger med status `testing`")
          }
        }
        .getOrElse {
          logger.error(
              "klarte ikke å oppdatere status for målinger med status `testing` i maalingDAO", it)
        }
  }

  private fun updateCrawlingStatuses(maaling: Maaling): Result<Maaling> =
      when (maaling) {
        is Maaling.Crawling -> {
          runCatching {
            val oppdaterteResultater =
                maaling.crawlResultat
                    .filterIsInstance<CrawlResultat.IkkeFerdig>()
                    .map {
                      crawlerClient.getStatus(it).map { newStatus -> updateStatus(it, newStatus) }
                    }
                    .toSingleResult()
                    .getOrThrow()
            val oppdatertMaaling = maaling.copy(crawlResultat = oppdaterteResultater)
            Maaling.toKvalitetssikring(oppdatertMaaling) ?: oppdatertMaaling
          }
        }
        else -> Result.success(maaling)
      }

  private fun updateTestingStatuses(maaling: Maaling.Testing): Maaling {
    val oppdaterteTestKoeyringar =
        maaling.testKoeyringar.map { testKoeyring ->
          autoTesterClient.updateStatus(testKoeyring).getOrElse {
            logger.error(
                "feila da eg forsøkte å hente test status for løysing id ${testKoeyring.loeysing.id} på måling id ${maaling.id}",
                it)
            testKoeyring
          }
        }
    val oppdatertMaaling = maaling.copy(testKoeyringar = oppdaterteTestKoeyringar)
    return Maaling.toTestingFerdig(oppdatertMaaling) ?: oppdatertMaaling
  }
}
