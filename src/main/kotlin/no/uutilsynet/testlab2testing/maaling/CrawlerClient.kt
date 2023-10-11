package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import java.time.Instant
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "crawler")
data class CrawlerProperties(val url: String, val code: String)

@Component
class CrawlerClient(val crawlerProperties: CrawlerProperties, val restTemplate: RestTemplate) {

  private val logger = LoggerFactory.getLogger(CrawlerClient::class.java)

  suspend fun start(maaling: Maaling.Planlegging): Maaling.Crawling = coroutineScope {
    val deferreds: List<Deferred<CrawlResultat>> =
        maaling.loeysingList.map { loeysing -> async { start(loeysing, maaling.crawlParameters) } }
    val crawlResultatList = deferreds.awaitAll()
    Maaling.toCrawling(maaling, crawlResultatList)
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
    return Maaling.Crawling(
        id = maaling.id,
        navn = maaling.navn,
        datoStart = maaling.datoStart,
        crawlResultat = crawlResultat)
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
            CrawlResultat.IkkjeStarta(statusUris.statusQueryGetUri.toURL(), loeysing, Instant.now())
          }
          .getOrElse { exception ->
            logger.error(
                "feilet da jeg forsøkte å starte crawling for løysing ${loeysing.id}", exception)
            CrawlResultat.Feila(
                exception.message ?: "start crawling feilet", loeysing, Instant.now())
          }

  fun getStatus(crawlResultat: CrawlResultat.Starta, maalingId: Int): Result<CrawlStatus> =
      fetchCrawlerStatus(crawlResultat.statusUrl.toURI(), crawlResultat.loeysing.id, maalingId)

  fun getStatus(crawlResultat: CrawlResultat.IkkjeStarta, maalingId: Int): Result<CrawlStatus> =
      fetchCrawlerStatus(crawlResultat.statusUrl.toURI(), crawlResultat.loeysing.id, maalingId)

  fun fetchCrawlerStatus(uri: URI, maalingId: Int, loeysingId: Int): Result<CrawlStatus> =
      runCatching { restTemplate.getForObject(uri, CrawlStatus::class.java)!! }
          .onFailure {
            logger.error(
                "feilet da jeg forsøkte å hente status crawling $uri for måling id $maalingId løysing id $loeysingId",
                it)
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
    JsonSubTypes.Type(value = CrawlStatus.Failed::class, name = "Failed"),
    JsonSubTypes.Type(value = CrawlStatus.Terminated::class, name = "Terminated"))
sealed class CrawlStatus {
  object Pending : CrawlStatus()

  data class Running(val customStatus: CustomStatus) : CrawlStatus()

  data class Completed(val output: List<CrawlerOutput>) : CrawlStatus()

  data class Failed(val output: String) : CrawlStatus()

  object Terminated : CrawlStatus()
}
