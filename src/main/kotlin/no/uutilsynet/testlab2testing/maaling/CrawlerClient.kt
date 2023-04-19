package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "crawler")
data class CrawlerProperties(val url: String, val code: String)

@Component
class CrawlerClient(val crawlerProperties: CrawlerProperties, val restTemplate: RestTemplate) {

  private val logger = LoggerFactory.getLogger(CrawlerClient::class.java)

  fun start(maaling: Maaling.Planlegging): Maaling.Crawling {
    val crawlResultat = maaling.loeysingList.map { start(it, maaling.crawlParameters) }
    return Maaling.toCrawling(maaling, crawlResultat)
  }

  fun restart(
      maaling: Maaling.Kvalitetssikring,
      loeysingIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Maaling.Crawling {
    val crawlResultat =
        maaling.crawlResultat.map {
          if (it.loeysing.id in loeysingIdList) start(it.loeysing, crawlParameters) else it
        }
    return Maaling.Crawling(id = maaling.id, navn = maaling.navn, crawlResultat = crawlResultat)
  }

  private fun start(loeysing: Loeysing, crawlParameters: CrawlParameters): CrawlResultat =
      runCatching {
            val statusUris =
                restTemplate.postForObject(
                    "${crawlerProperties.url}?code=${crawlerProperties.code}",
                    mapOf(
                        "startUrl" to loeysing.url,
                        "maxLenker" to crawlParameters.maxLinksPerPage,
                        "talLenker" to crawlParameters.numLinksToSelect,
                        "idLoeysing" to loeysing.id,
                        "domene" to loeysing.url),
                    AutoTesterClient.StatusUris::class.java)!!
            CrawlResultat.IkkeFerdig(statusUris.statusQueryGetUri.toURL(), loeysing, Instant.now())
          }
          .getOrElse { exception ->
            logger.error(
                "feilet da jeg forsøkte å starte crawling for løysing ${loeysing.id}", exception)
            CrawlResultat.Feilet(
                exception.message ?: "start crawling feilet", loeysing, Instant.now())
          }

  fun getStatus(crawlResultat: CrawlResultat): Result<CrawlStatus> =
      when (crawlResultat) {
        is CrawlResultat.IkkeFerdig -> {
          data class CrawlerOutput(val url: String, val title: String)
          data class StatusDTO(val runtimeStatus: String, val output: List<CrawlerOutput>?)

          runCatching {
            val response =
                restTemplate.getForObject(crawlResultat.statusUrl.toURI(), StatusDTO::class.java)!!
            when (response.runtimeStatus) {
              "Pending" -> CrawlStatus.Pending
              "Running" -> CrawlStatus.Running
              "Completed" -> {
                val nettsider =
                    response.output?.map { crawlerOutput -> URL(crawlerOutput.url) }
                        ?: throw RuntimeException("`output` fra crawler er `null`")
                CrawlStatus.Completed(nettsider)
              }
              "Failed" -> CrawlStatus.Failed
              else -> throw RuntimeException("ukjent status: ${response.runtimeStatus}")
            }
          }
        }
        is CrawlResultat.Feilet -> Result.success(CrawlStatus.Failed)
        is CrawlResultat.Ferdig -> Result.success(CrawlStatus.Completed(crawlResultat.nettsider))
      }
}

sealed class CrawlStatus {
  object Pending : CrawlStatus()
  object Running : CrawlStatus()
  data class Completed(val nettsider: List<URL>) : CrawlStatus()
  object Failed : CrawlStatus()
}
