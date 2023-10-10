package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
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
    Type(CrawlResultat.IkkjeStarta::class, name = "ikkje_starta"),
    Type(CrawlResultat.Starta::class, name = "starta"),
    Type(CrawlResultat.Ferdig::class, name = "ferdig"),
    Type(CrawlResultat.Feila::class, name = "feila"))
sealed class CrawlResultat {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant

  data class IkkjeStarta(
      val statusUrl: URL,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
  ) : CrawlResultat()

  data class Starta(
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

  data class Feila(
      val feilmelding: String,
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant
  ) : CrawlResultat()
}

fun updateStatus(crawlResultat: CrawlResultat, newStatus: CrawlStatus): CrawlResultat =
    when (crawlResultat) {
      is CrawlResultat.IkkjeStarta,
      is CrawlResultat.Starta -> {
        when (newStatus) {
          is CrawlStatus.Pending ->
              CrawlResultat.IkkjeStarta(
                  statusUrl =
                      if (crawlResultat is CrawlResultat.Starta) crawlResultat.statusUrl
                      else (crawlResultat as CrawlResultat.IkkjeStarta).statusUrl,
                  crawlResultat.loeysing,
                  Instant.now())
          is CrawlStatus.Running ->
              if (newStatus.customStatus == null) {
                crawlResultat
              } else {
                when (crawlResultat) {
                  is CrawlResultat.Starta ->
                      crawlResultat.copy(framgang = Framgang.from(newStatus.customStatus))
                  is CrawlResultat.IkkjeStarta ->
                      CrawlResultat.Starta(
                          crawlResultat.statusUrl,
                          crawlResultat.loeysing,
                          Instant.now(),
                          framgang = Framgang.from(newStatus.customStatus))
                  else ->
                      throw RuntimeException(
                          "Ulovleg status på crawlresultat for løysing id ${crawlResultat.loeysing.id}, kan ikkje oppdatere ny status")
                }
              }
          is CrawlStatus.Completed ->
              if (newStatus.output.isEmpty()) {
                CrawlResultat.Feila(
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

                if (invalidUrls.isNotEmpty()) {
                  logger.warn("${invalidUrls.size} ugyldige urler oppdaget i oppdatering av status")
                }
                CrawlResultat.Ferdig(
                    validUrls.size,
                    statusUrl =
                        if (crawlResultat is CrawlResultat.Starta) crawlResultat.statusUrl
                        else (crawlResultat as CrawlResultat.IkkjeStarta).statusUrl,
                    crawlResultat.loeysing,
                    Instant.now(),
                    validUrls.map { it.getOrThrow() },
                )
              }
          is CrawlStatus.Failed ->
              CrawlResultat.Feila(newStatus.output, crawlResultat.loeysing, Instant.now())
          is CrawlStatus.Terminated ->
              CrawlResultat.Feila(
                  "Crawling av ${crawlResultat.loeysing.url} ble avbrutt.",
                  crawlResultat.loeysing,
                  Instant.now())
        }
      }
      else -> crawlResultat
    }
