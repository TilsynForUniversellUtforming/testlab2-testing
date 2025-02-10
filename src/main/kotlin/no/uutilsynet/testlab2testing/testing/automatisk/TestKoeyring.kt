package no.uutilsynet.testlab2testing.testing.automatisk

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.forenkletkontroll.Framgang
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tilstand")
@JsonSubTypes(
    Type(TestKoeyring.IkkjeStarta::class, name = "ikkje_starta"),
    Type(TestKoeyring.Starta::class, name = "starta"),
    Type(TestKoeyring.Ferdig::class, name = "ferdig"),
    Type(TestKoeyring.Feila::class, name = "feila"))
sealed class TestKoeyring {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant
  abstract val brukar: Brukar?

  data class IkkjeStarta(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      override val brukar: Brukar?,
      val antallNettsider: Int,
  ) : TestKoeyring()

  data class Starta(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      val framgang: Framgang,
      override val brukar: Brukar?,
      val antallNettsider: Int,
  ) : TestKoeyring()

  data class Ferdig(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      @JsonIgnore val lenker: AutoTesterClient.AutoTesterLenker? = null,
      override val brukar: Brukar?,
      val antallNettsider: Int,
  ) : TestKoeyring()

  data class Feila(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val feilmelding: String,
      override val brukar: Brukar?
  ) : TestKoeyring()

  companion object {
    fun from(crawlResultat: CrawlResultat.Ferdig, statusURL: URL, brukar: Brukar?): IkkjeStarta =
        IkkjeStarta(
            crawlResultat.loeysing, Instant.now(), statusURL, brukar, crawlResultat.antallNettsider)

    fun from(
        loeysing: Loeysing,
        statusURL: URL,
        brukar: Brukar?,
        antallNettsider: Int
    ): IkkjeStarta = IkkjeStarta(loeysing, Instant.now(), statusURL, brukar, antallNettsider)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.AutoTesterStatus
    ): TestKoeyring =
        if (response is AutoTesterClient.AutoTesterStatus.Terminated) {
          Feila(
              testKoeyring.loeysing,
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
                  testKoeyring.loeysing,
                  Instant.now(),
                  testKoeyring.statusURL,
                  testKoeyring.brukar,
                  testKoeyring.antallNettsider)
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.loeysing,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output,
                  testKoeyring.brukar,
                  testKoeyring.antallNettsider)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(testKoeyring.loeysing, Instant.now(), response.output, testKoeyring.brukar)
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.loeysing,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.antallNettsider),
                  testKoeyring.brukar,
                  testKoeyring.antallNettsider)
          else -> testKoeyring
        }

    private fun updateStatusIkkjeStarta(
        response: AutoTesterClient.AutoTesterStatus,
        testKoeyring: IkkjeStarta
    ) =
        when (response) {
          is AutoTesterClient.AutoTesterStatus.Running ->
              Starta(
                  testKoeyring.loeysing,
                  Instant.now(),
                  testKoeyring.statusURL,
                  Framgang.from(response.customStatus, testKoeyring.antallNettsider),
                  testKoeyring.brukar,
                  testKoeyring.antallNettsider)
          is AutoTesterClient.AutoTesterStatus.Completed ->
              Ferdig(
                  testKoeyring.loeysing,
                  Instant.now(),
                  testKoeyring.statusURL,
                  response.output,
                  testKoeyring.brukar,
                  testKoeyring.antallNettsider)
          is AutoTesterClient.AutoTesterStatus.Failed ->
              Feila(testKoeyring.loeysing, Instant.now(), response.output, testKoeyring.brukar)
          else -> testKoeyring
        }
  }
}
