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
  abstract val crawlResultat: CrawlResultat.Ferdig
  abstract val sistOppdatert: Instant

  data class IkkjeStarta(
      @JsonIgnore override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Starta(
      @JsonIgnore override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      val framgang: Framgang
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Ferdig(
      @JsonIgnore override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val statusURL: URL,
      @JsonIgnore val testResultat: List<TestResultat>,
      @JsonIgnore val lenker: AutoTesterClient.AutoTesterOutput.Lenker? = null
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  data class Feila(
      @JsonIgnore override val crawlResultat: CrawlResultat.Ferdig,
      override val sistOppdatert: Instant,
      val feilmelding: String
  ) : TestKoeyring() {
    override val loeysing: Loeysing
      get() = crawlResultat.loeysing
  }

  companion object {
    fun from(crawlResultat: CrawlResultat.Ferdig, statusURL: URL): TestKoeyring =
        IkkjeStarta(crawlResultat, Instant.now(), statusURL)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.AzureFunctionResponse
    ): TestKoeyring =
        when (testKoeyring) {
          is IkkjeStarta ->
              when (response) {
                is AutoTesterClient.AzureFunctionResponse.Running ->
                    Starta(
                        testKoeyring.crawlResultat,
                        Instant.now(),
                        testKoeyring.statusURL,
                        Framgang.from(
                            response.customStatus, testKoeyring.crawlResultat.nettsider.size))
                is AutoTesterClient.AzureFunctionResponse.Completed ->
                    when (response.output) {
                      is AutoTesterClient.AutoTesterOutput.Lenker ->
                          Ferdig(
                              testKoeyring.crawlResultat,
                              Instant.now(),
                              testKoeyring.statusURL,
                              emptyList(),
                              response.output)
                      is AutoTesterClient.AutoTesterOutput.TestResultater ->
                          Ferdig(
                              testKoeyring.crawlResultat,
                              Instant.now(),
                              testKoeyring.statusURL,
                              response.output.testResultater)
                    }
                is AutoTesterClient.AzureFunctionResponse.Failed ->
                    Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
                else -> testKoeyring
              }
          is Starta -> {
            when (response) {
              is AutoTesterClient.AzureFunctionResponse.Pending ->
                  IkkjeStarta(testKoeyring.crawlResultat, Instant.now(), testKoeyring.statusURL)
              is AutoTesterClient.AzureFunctionResponse.Completed ->
                  when (response.output) {
                    is AutoTesterClient.AutoTesterOutput.Lenker ->
                        Ferdig(
                            testKoeyring.crawlResultat,
                            Instant.now(),
                            testKoeyring.statusURL,
                            emptyList(),
                            response.output)
                    is AutoTesterClient.AutoTesterOutput.TestResultater ->
                        Ferdig(
                            testKoeyring.crawlResultat,
                            Instant.now(),
                            testKoeyring.statusURL,
                            response.output.testResultater)
                  }
              is AutoTesterClient.AzureFunctionResponse.Failed ->
                  Feila(testKoeyring.crawlResultat, Instant.now(), response.output)
              else -> testKoeyring
            }
          }
          else -> testKoeyring
        }

    fun aggregerPaaTestregel(
        koeyringarMedResultat: Map<Ferdig, List<TestResultat>>,
        maalingId: Int
    ): List<AggregertResultat> =
        koeyringarMedResultat.flatMap { (testKoeyring, resultat) ->
          aggregerPaaTestregel(resultat, testKoeyring, maalingId)
        }

    private fun aggregerPaaTestregel(
        testResultat: List<TestResultat>,
        testKoeyring: Ferdig,
        maalingId: Int
    ): List<AggregertResultat> {
      val antallSider = testKoeyring.crawlResultat.nettsider.size
      return testResultat
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
            val antallSiderUtenForekomst = antallSider - siderTestet
            // suksesskriterier vil være det samme for alle testresultatene, siden resultatene er
            // gruppert på testregelId
            val alleSuksesskriterier = entry.value.first().suksesskriterium

            AggregertResultat(
                maalingId,
                testKoeyring.loeysing,
                entry.key,
                alleSuksesskriterier.first(),
                alleSuksesskriterier.drop(1),
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
      val suksesskriterium: String,
      val fleireSuksesskriterium: List<String>,
      val talElementSamsvar: Int,
      val talElementBrot: Int,
      val talElementVarsel: Int,
      val talSiderSamsvar: Int,
      val talSiderBrot: Int,
      val talSiderIkkjeForekomst: Int
  )
}
