package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
            CrawlResultat.IkkeFerdig(
                statusUris.statusQueryGetUri.toURL(),
                loeysing,
                Instant.now(),
                CrawlResultat.Framgang(0, crawlParameters.maxLinksPerPage))
          }
          .getOrElse { exception ->
            logger.error(
                "feilet da jeg forsøkte å starte crawling for løysing ${loeysing.id}", exception)
            CrawlResultat.Feilet(
                exception.message ?: "start crawling feilet", loeysing, Instant.now())
          }

  fun getStatus(crawlResultat: CrawlResultat.IkkeFerdig): Result<CrawlStatus> = runCatching {
    val response =
        restTemplate.getForObject(crawlResultat.statusUrl.toURI(), CrawlStatus::class.java)!!
    response
  }
}

data class CrawlerOutput(val url: String, val title: String)

data class CustomStatus(val lenkerCrawla: Int, val maxLenker: Int)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "runtimeStatus")
@JsonSubTypes(
    JsonSubTypes.Type(value = CrawlStatus.Pending::class, name = "Pending"),
    JsonSubTypes.Type(value = CrawlStatus.Running::class, name = "Running"),
    JsonSubTypes.Type(value = CrawlStatus.Completed::class, name = "Completed"),
    JsonSubTypes.Type(value = CrawlStatus.Failed::class, name = "Failed"))
sealed class CrawlStatus {
  object Pending : CrawlStatus()
  data class Running(val customStatus: CustomStatus?) : CrawlStatus()
  data class Completed(val output: List<CrawlerOutput>) : CrawlStatus()
  data class Failed(val output: String) : CrawlStatus()
}
