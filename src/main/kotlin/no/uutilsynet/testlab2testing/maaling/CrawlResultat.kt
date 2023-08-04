package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing

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
      val nettsider: List<URL>,
      val statusUrl: URL,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant
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
                CrawlResultat.Ferdig(
                    newStatus.output.map { crawlerOutput -> URI(crawlerOutput.url).toURL() },
                    crawlResultat.statusUrl,
                    crawlResultat.loeysing,
                    Instant.now())
              }
          is CrawlStatus.Failed ->
              CrawlResultat.Feilet(
                  "Crawling av ${crawlResultat.loeysing.url} feilet.",
                  crawlResultat.loeysing,
                  Instant.now())
          is CrawlStatus.Terminated ->
              CrawlResultat.Feilet(
                  "Crawling av ${crawlResultat.loeysing.url} ble avbrutt.",
                  crawlResultat.loeysing,
                  Instant.now())
        }
      }
      else -> crawlResultat
    }
