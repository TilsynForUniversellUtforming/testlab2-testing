package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger(CrawlResultat::class.java)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CrawlResultat.IkkeFerdig::class, name = "ikke_ferdig"),
    JsonSubTypes.Type(CrawlResultat.Feilet::class, name = "feilet"),
    JsonSubTypes.Type(CrawlResultat.Ferdig::class, name = "ferdig"))
sealed class CrawlResultat {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant

  data class IkkeFerdig(
      val statusUrl: URL,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val framgang: Framgang
  ) : CrawlResultat()

  data class Ferdig(
      val antallNettsider: Int,
      val statusUrl: URL,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      @JsonIgnore val nettsider: List<URL> = emptyList(),
  ) : CrawlResultat()

  data class Feilet(
      val feilmelding: String,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant
  ) : CrawlResultat()
}

fun updateStatus(crawlResultat: CrawlResultat, newStatus: CrawlStatus): CrawlResultat =
    when (crawlResultat) {
      is CrawlResultat.IkkeFerdig -> {
        when (newStatus) {
          is CrawlStatus.Pending -> crawlResultat
          is CrawlStatus.Running ->
              if (newStatus.customStatus == null) {
                crawlResultat
              } else {
                crawlResultat.copy(framgang = Framgang.from(newStatus.customStatus))
              }
          is CrawlStatus.Completed ->
              if (newStatus.output.isEmpty()) {
                CrawlResultat.Feilet(
                    "Crawling av ${crawlResultat.loeysing.url} feilet. Output fra crawleren var en tom liste.",
                    crawlResultat.loeysing,
                    Instant.now())
              } else {
                val urlList = newStatus.output.map { runCatching { URI(it.url).toURL() } }
                urlList.forEach {
                  if (it.isFailure)
                      logger.info(
                          "Crawlresultatet for løysing ${crawlResultat.loeysing.id} inneholder en ugyldig url: ${it.exceptionOrNull()?.message}")
                }
                val (validUrls, invalidUrls) = urlList.partition { it.isSuccess }
                logger.warn("${invalidUrls.size} ugyldige urler oppdaget i oppdatering av status")
                CrawlResultat.Ferdig(
                    validUrls.size,
                    crawlResultat.statusUrl,
                    crawlResultat.loeysing,
                    Instant.now(),
                    validUrls.map { it.getOrThrow() },
                )
              }
          is CrawlStatus.Failed ->
              CrawlResultat.Feilet(newStatus.output, crawlResultat.loeysing, Instant.now())
          is CrawlStatus.Terminated ->
              CrawlResultat.Feilet(
                  "Crawling av ${crawlResultat.loeysing.url} ble avbrutt.",
                  crawlResultat.loeysing,
                  Instant.now())
        }
      }
      else -> crawlResultat
    }
