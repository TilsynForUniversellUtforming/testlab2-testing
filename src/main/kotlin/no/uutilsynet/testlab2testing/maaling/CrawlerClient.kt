package no.uutilsynet.testlab2testing.maaling

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
    val crawlResultat = maaling.loeysingList.map { start(it) }
    return Maaling.toCrawling(maaling, crawlResultat)
  }

  private fun start(loeysing: Loeysing): CrawlResultat =
      runCatching {
            val statusUris =
                restTemplate.postForObject(
                    "${crawlerProperties.url}?code=${crawlerProperties.code}",
                    mapOf(
                        "startUrl" to loeysing.url,
                        "maxLenker" to 10,
                        "talLenker" to 10,
                        "idLoeysing" to loeysing.id,
                        "domene" to loeysing.url),
                    AutoTesterAdapter.StatusUris::class.java)!!
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
          data class StatusDTO(val runtimeStatus: String)

          runCatching {
                val response =
                    restTemplate.getForObject(
                        crawlResultat.statusUrl.toURI(), StatusDTO::class.java)!!
                return Result.success(CrawlStatus.valueOf(response.runtimeStatus))
              }
              .getOrElse {
                logger.warn("problemer med å hente status for et crawlresultat", it)
                Result.failure(it)
              }
        }
        is CrawlResultat.Feilet -> Result.success(CrawlStatus.Failed)
        else -> Result.success(CrawlStatus.Completed)
      }
}

enum class CrawlStatus {
  Pending,
  Running,
  Completed,
  Failed
}
