package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tilstand")
@JsonSubTypes(
    Type(TestKoeyring.IkkjeStarta::class, name = "ikkje_starta"),
    Type(TestKoeyring.Starta::class, name = "starta"),
    Type(TestKoeyring.Ferdig::class, name = "ferdig"),
    Type(TestKoeyring.Feila::class, name = "feila"))
sealed class TestKoeyring {
  abstract val loeysing: Loeysing
  abstract val crawlResultat: CrawlResultat
  abstract val sistOppdatert: Instant

  data class IkkjeStarta(
      @JsonIgnore override val crawlResultat: CrawlResultat,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Starta(
      @JsonIgnore override val crawlResultat: CrawlResultat,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Ferdig(
      @JsonIgnore override val crawlResultat: CrawlResultat,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      @JsonIgnore val testResultat: List<TestResultat>
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Feila(
      @JsonIgnore override val crawlResultat: CrawlResultat,
      override val sistOppdatert: Instant,
      val feilmelding: String
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  companion object {
    fun from(crawlResultat: CrawlResultat, statusURL: URL): TestKoeyring =
        IkkjeStarta(crawlResultat, Instant.now(), statusURL)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.AzureFunctionResponse
    ): TestKoeyring =
        when (testKoeyring) {
          is IkkjeStarta ->
              when (response) {
                is AutoTesterClient.AzureFunctionResponse.Running ->
                    Starta(testKoeyring.crawlResultat, Instant.now(), testKoeyring.statusURL)
                is AutoTesterClient.AzureFunctionResponse.Completed ->
                    Ferdig(
                        testKoeyring.crawlResultat,
                        Instant.now(),
                        testKoeyring.statusURL,
                        response.output)
                is AutoTesterClient.AzureFunctionResponse.Failed ->
                    Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
                else -> testKoeyring
              }
          is Starta -> {
            when (response) {
              is AutoTesterClient.AzureFunctionResponse.Pending ->
                  IkkjeStarta(testKoeyring.crawlResultat, Instant.now(), testKoeyring.statusURL)
              is AutoTesterClient.AzureFunctionResponse.Completed ->
                  Ferdig(
                      testKoeyring.crawlResultat,
                      Instant.now(),
                      testKoeyring.statusURL,
                      response.output)
              is AutoTesterClient.AzureFunctionResponse.Failed ->
                  Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
              else -> testKoeyring
            }
          }
          else -> testKoeyring
        }

    fun aggregerPaaTestregel(
        testKoeyringar: List<Ferdig>,
        maalingId: Int,
        loeysing: Loeysing
    ): List<AggregertResultat> {
      val totaltAntallSider =
          testKoeyringar
              .map { it.crawlResultat }
              .filterIsInstance<CrawlResultat.Ferdig>()
              .flatMap { it.nettsider }
              .map { it.toString() }
              .distinct()
              .count()
      return testKoeyringar
          .flatMap { it.testResultat }
          .groupBy { it.testregelId }
          .map { entry ->
            val antallSamsvar = entry.value.count { it.elementResultat == "samsvar" }
            val antallBrot = entry.value.count { it.elementResultat == "brot" }
            val antallVarsel = entry.value.count { it.elementResultat == "varsel" }
            val antallSiderMedSamsvar =
                entry.value
                    .groupBy { it.side }
                    .count {
                      it.value.all { testResultat -> testResultat.elementResultat == "samsvar" }
                    }
            val antallSiderMedBrot =
                entry.value
                    .groupBy { it.side }
                    .count {
                      it.value.any { testResultat -> testResultat.elementResultat == "brot" }
                    }
            val siderTestet = entry.value.map { it.side }.distinct().count()
            val antallSiderUtenForekomst = totaltAntallSider - siderTestet

            AggregertResultat(
                maalingId,
                loeysing,
                entry.key,
                antallSamsvar,
                antallBrot,
                antallVarsel,
                antallSiderMedSamsvar,
                antallSiderMedBrot,
                antallSiderUtenForekomst)
          }
    }
  }

  data class AggregertResultat(
      val maalingId: Int,
      val loeysing: Loeysing,
      val testregelId: String,
      val antallSamsvar: Int,
      val antallBrot: Int,
      val antallVarsel: Int,
      val antallSiderMedSamsvar: Int,
      val antallSiderMedBrot: Int,
      val antallSiderUtenForekomst: Int
  )
}
