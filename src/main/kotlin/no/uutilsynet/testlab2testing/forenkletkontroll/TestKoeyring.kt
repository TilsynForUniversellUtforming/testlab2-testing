package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
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

  data class IkkjeStarta(
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Starta(
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      val framgang: Framgang
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Ferdig(
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      @JsonIgnore val lenker: AutoTesterClient.AutoTesterLenker? = null
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Feila(
      override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val feilmelding: String
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  companion object {
    fun from(crawlResultat: CrawlResultat.Ferdig, statusURL: URL): IkkjeStarta =
        IkkjeStarta(crawlResultat, Instant.now(), statusURL)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.AutoTesterStatus
    ): TestKoeyring =
        if (response is AutoTesterClient.AutoTesterStatus.Terminated) {
          Feila(testKoeyring.crawlResultat, Instant.now(), "Testen har blitt stoppa manuelt.")
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
              IkkjeStarta(testKoeyring.crawlResultat, Instant.now(), testKoeyring.statusURL)
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.crawlResultat.antallNettsider))
          else -> testKoeyring
        }

    private fun updateStatusIkkjeStarta(
        response: AutoTesterClient.AutoTesterStatus,
        testKoeyring: IkkjeStarta
    ) =
        when (response) {
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.crawlResultat.antallNettsider))
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.crawlResultat,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
          else -> testKoeyring
        }
  }
}
