package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.dto.Loeysing
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "crawler")
data class CrawlerProperties(val url: String, val code: String)

@Component
class Crawler(val crawlerProperties: CrawlerProperties, val restTemplate: RestTemplate) {
  fun start(maaling: Maaling.Planlegging): List<CrawlResultat> =
      maaling.loeysingList.map { start(it) }

  private fun start(loeysing: Loeysing): CrawlResultat =
      runCatching {
            val statusUris =
                restTemplate.postForObject(
                    "${crawlerProperties.url}?code=${crawlerProperties.code}",
                    mapOf(
                        "startUrl" to loeysing.url,
                        "maxLenker" to 100,
                        "talLenker" to 30,
                        "domene" to loeysing.url),
                    AutoTesterAdapter.StatusUris::class.java)!!
            CrawlResultat.IkkeFerdig(statusUris.statusQueryGetUri.toURL(), loeysing)
          }
          .getOrElse { exception ->
            CrawlResultat.Feilet(exception.message ?: "start crawling feilet", loeysing)
          }
}
