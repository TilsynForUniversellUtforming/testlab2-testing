package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(CrawlResultat.IkkeFerdig::class, name = "ikke_ferdig"),
    JsonSubTypes.Type(CrawlResultat.Feilet::class, name = "feilet"),
    JsonSubTypes.Type(CrawlResultat.Ferdig::class, name = "ferdig"))
sealed class CrawlResultat {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant

  data class Framgang(val lenkerCrawla: Int, val maxLenker: Int) {
    companion object {
      fun from(customStatus: CustomStatus): Framgang =
          Framgang(customStatus.lenkerCrawla, customStatus.maxLenker)
    }
  }

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
                crawlResultat.copy(framgang = CrawlResultat.Framgang.from(newStatus.customStatus))
              }
          is CrawlStatus.Completed ->
              CrawlResultat.Ferdig(
                  newStatus.output.map { crawlerOutput -> URL(crawlerOutput.url) },
                  crawlResultat.statusUrl,
                  crawlResultat.loeysing,
                  Instant.now())
          is CrawlStatus.Failed ->
              CrawlResultat.Feilet(
                  "Crawling av ${crawlResultat.loeysing.url} feilet.",
                  crawlResultat.loeysing,
                  Instant.now())
        }
      }
      else -> crawlResultat
    }
