package no.uutilsynet.testlab2testing.forenkletkontroll

import kotlinx.coroutines.*
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlerClient
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MaalingCrawlingService(
    val crawlerClient: CrawlerClient,
    val maalingService: MaalingService,
    val maalingDAO: MaalingDAO
) {

  fun restartCrawling(
      statusDTO: MaalingResource.StatusDTO,
      maaling: Maaling.Kvalitetssikring
  ): ResponseEntity<Any> {
    val loeysingIdList = maalingService.getValidatedLoeysingList(statusDTO)
    val crawlParameters = maalingDAO.getCrawlParameters(maaling.id)
    val updated = restartCrawling(maaling, loeysingIdList, crawlParameters)
    maalingDAO.save(updated).getOrThrow()
    return ResponseEntity.ok().build()
  }

  fun restartCrawling(
      maaling: Maaling.Kvalitetssikring,
      loeysingIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Maaling.Crawling {
    val crawlResultat =
        maaling.crawlResultat.map {
          if (it.loeysing.id in loeysingIdList) crawlerClient.start(it.loeysing, crawlParameters)
          else it
        }
    return Maaling.Crawling(
        id = maaling.id,
        navn = maaling.navn,
        datoStart = maaling.datoStart,
        crawlResultat = crawlResultat)
  }

  suspend fun startCrawling(maaling: Maaling.Planlegging): ResponseEntity<Any> {
    val updated = startCrawlingMaaling(maaling)
    withContext(Dispatchers.IO) { maalingDAO.save(updated) }.getOrThrow()
    return ResponseEntity.ok().build()
  }

  private suspend fun startCrawlingMaaling(maaling: Maaling.Planlegging): Maaling.Crawling =
      coroutineScope {
        val deferreds: List<Deferred<CrawlResultat>> =
            maaling.loeysingList.map { loeysing ->
              async { crawlerClient.start(loeysing, maaling.crawlParameters) }
            }
        val crawlResultatList = deferreds.awaitAll()
        Maaling.toCrawling(maaling, crawlResultatList)
      }
}
