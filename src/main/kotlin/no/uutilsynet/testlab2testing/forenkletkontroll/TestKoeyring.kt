package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.loeysing.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tilstand")
@JsonSubTypes(
    Type(TestKoeyring.IkkjeStarta::class, name = "ikkje_starta"),
    Type(TestKoeyring.Starta::class, name = "starta"),
    Type(TestKoeyring.Ferdig::class, name = "ferdig"),
    Type(TestKoeyring.Feila::class, name = "feila"))
sealed class TestKoeyring {
  abstract val loeysing: Loeysing
  abstract val crawlResultat: CrawlResultat.Ferdig
  abstract val sistOppdatert: Instant
  abstract val brukar: Brukar?

  data class IkkjeStarta(
      override val loeysing: Loeysing,
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      override val brukar: Brukar?
  ) : TestKoeyring()

  data class Starta(
      override val loeysing: Loeysing,
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      val framgang: Framgang,
      override val brukar: Brukar?
  ) : TestKoeyring()

  data class Ferdig(
      override val loeysing: Loeysing,
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      @JsonIgnore val lenker: AutoTesterClient.AutoTesterLenker? = null,
      override val brukar: Brukar?
  ) : TestKoeyring()

  data class Feila(
      override val loeysing: Loeysing,
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val feilmelding: String,
      override val brukar: Brukar?
  ) : TestKoeyring()

  companion object {
    fun from(crawlResultat: CrawlResultat.Ferdig, statusURL: URL, brukar: Brukar?): IkkjeStarta =
        IkkjeStarta(crawlResultat.loeysing, crawlResultat, Instant.now(), statusURL, brukar)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.AutoTesterStatus
    ): TestKoeyring =
        if (response is AutoTesterClient.AutoTesterStatus.Terminated) {
          Feila(
              testKoeyring.crawlResultat.loeysing,
              testKoeyring.crawlResultat,
              Instant.now(),
              "Testen har blitt stoppa manuelt.",
              testKoeyring.brukar)
        } else {
          when (testKoeyring) {
            is IkkjeStarta -> updateStatusIkkjeStarta(response, testKoeyring)
            is Starta -> {
              updateStatusStarta(response, testKoeyring)
            }
            else -> testKoeyring
          }
        }

    private fun updateStatusStarta(
        response: AutoTesterClient.AutoTesterStatus,
        testKoeyring: Starta
    ) =
        when (response) {
          is AutoTesterClient.AutoTesterStatus.Pending ->
              IkkjeStarta(
                  testKoeyring.crawlResultat.loeysing,
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.crawlResultat.loeysing,
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output,
                  testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(testKoeyring.crawlResultat.loeysing,testKoeyring.crawlResultat, Instant.now(), response.output, testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.crawlResultat.loeysing,
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.crawlResultat.antallNettsider),
                  testKoeyring.brukar)
          else -> testKoeyring
        }

    private fun updateStatusIkkjeStarta(
        response: AutoTesterClient.AutoTesterStatus,
        testKoeyring: IkkjeStarta
    ) =
        when (response) {
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.crawlResultat.loeysing,
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.crawlResultat.antallNettsider),
                  testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.crawlResultat.loeysing,
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output,
                  testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(
                  testKoeyring.crawlResultat.loeysing,testKoeyring.crawlResultat, Instant.now(), response.output, testKoeyring.brukar)
          else -> testKoeyring
        }
  }
}
