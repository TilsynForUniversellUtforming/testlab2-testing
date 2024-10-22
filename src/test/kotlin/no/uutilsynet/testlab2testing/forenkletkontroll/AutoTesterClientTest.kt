package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.Instant
import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.statusURL
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.testRegelList
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators

@RestClientTest(AutoTesterClient::class, AutoTesterProperties::class)
class AutoTesterClientTest {
  @Autowired private lateinit var server: MockRestServiceServer
  @Autowired private lateinit var autoTesterClient: AutoTesterClient

  private val objectMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @DisplayName("parsing av responsen fra autotester")
  @Nested
  inner class ParsingResponse {
    @DisplayName("når responsen fra autotester er `Pending`, så skal det parses til responsklassen")
    @Test
    fun pending() {
      val jsonString = """{"runtimeStatus":"Pending", "output": null}"""
      val pending =
          objectMapper.readValue(jsonString, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(pending).isInstanceOf(AutoTesterClient.AutoTesterStatus.Pending::class.java)
    }

    @DisplayName("når responsen fra autotester er `Running`, så skal det parses til responsklassen")
    @Test
    fun running() {
      val jsonString =
          """{"runtimeStatus":"Running", "output": null, "customStatus":{"testaSider":0, "talSider":0}}"""
      val running =
          objectMapper.readValue(jsonString, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(running).isInstanceOf(AutoTesterClient.AutoTesterStatus.Running::class.java)

      val jsonStringNoStatus = """{"runtimeStatus":"Running", "output": null}"""
      val runningNoStatus =
          objectMapper.readValue(jsonStringNoStatus, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(runningNoStatus)
          .isInstanceOf(AutoTesterClient.AutoTesterStatus.Running::class.java)
    }

    @DisplayName(
        "når responsen fra autotester er `Completed`, og responsen inneholder urler til testresultater, så skal det parses til responsklassen")
    @Test
    fun completedWithURLs() {
      val jsonString =
          """{"runtimeStatus":"Completed", "output": {"urlFulltResultat": "https://fullt.resultat.no", "urlBrot": "https://brot.resultat.no","urlAggregeringTR": "https://aggregeringTR.resultat.no","urlAggregeringSK": "https://aggregeringSK.resultat.no","urlAggregeringSide": "https://aggregeringSide.resultat.no","urlAggregeringSideTR": "https://aggregeringSideTR.resultat.no","urlAggregeringLoeysing": "https://aggregeringLoeysing.resultat.no"}}"""
      val completed =
          objectMapper.readValue(jsonString, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(completed).isInstanceOf(AutoTesterClient.AutoTesterStatus.Completed::class.java)
      val output: AutoTesterClient.AutoTesterLenker =
          (completed as AutoTesterClient.AutoTesterStatus.Completed).output
      assertThat(output.urlFulltResultat).isEqualTo(URI("https://fullt.resultat.no").toURL())
      assertThat(output.urlBrot).isEqualTo(URI("https://brot.resultat.no").toURL())
      assertThat(output.urlAggregeringTR)
          .isEqualTo(URI("https://aggregeringTR.resultat.no").toURL())
    }

    @DisplayName("når responsen fra autotester er `Failed`, så skal det parses til responsklassen")
    @Test
    fun failed() {
      val jsonString = """{"runtimeStatus":"Failed", "output": "401 Unauthorized"}"""
      val failed = objectMapper.readValue(jsonString, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(failed).isInstanceOf(AutoTesterClient.AutoTesterStatus.Failed::class.java)
      assertThat((failed as AutoTesterClient.AutoTesterStatus.Failed).output)
          .isEqualTo("401 Unauthorized")
    }

    @DisplayName("når responsen fra autotester er `Terminated`, så skal det kunne parses")
    @Test
    fun terminated() {
      val outputFromAutotester = """{"runtimeStatus":"Terminated", "output": null}"""
      val terminated =
          objectMapper.readValue(
              outputFromAutotester, AutoTesterClient.AutoTesterStatus::class.java)
      assertThat(terminated).isInstanceOf(AutoTesterClient.AutoTesterStatus.Terminated::class.java)
    }
  }

  @DisplayName("Når man starter testing, skal den returnere statusQueryGetUri")
  @Test
  fun startTesting() {
    val maalingId = 1
    val crawlResultat = TestConstants.crawlResultat
    val nettsider =
        listOf(
            URI("https://www.uutilsynet.no/").toURL(),
            URI("https://www.uutilsynet.no/underside/1").toURL(),
            URI("https://www.uutilsynet.no/underside/2").toURL())

    val expectedRequestData =
        mapOf(
            "urls" to nettsider,
            "idMaaling" to maalingId,
            "idLoeysing" to crawlResultat.loeysing.id,
            "resultatSomFil" to true,
            "actRegler" to testRegelList.map { it.testregelSchema },
        )

    val statusUris = AutoTesterClient.StatusUris(URI(statusURL))
    val jsonResponse = jacksonObjectMapper().writeValueAsString(statusUris)

    server
        .expect(
            ExpectedCount.manyTimes(),
            MockRestRequestMatchers.requestTo(
                CoreMatchers.startsWith(autoTesterClient.autoTesterProperties.url)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andExpect(
            MockRestRequestMatchers.content()
                .json(objectMapper.writeValueAsString(expectedRequestData)))
        .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON))

    val result =
        autoTesterClient.startTesting(maalingId, crawlResultat.loeysing, testRegelList, nettsider)

    assertThat(result.isSuccess).isTrue
    assertThat(result.getOrNull()).isEqualTo(statusUris.statusQueryGetUri.toURL())
  }

  @DisplayName("Hvis det er feil i respons fra autotester skal man returnere Result.Failure")
  @Test
  fun updateStatusFailed() {
    server
        .expect(
            ExpectedCount.manyTimes(),
            MockRestRequestMatchers.requestTo(CoreMatchers.startsWith(statusURL)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(jsonFailure, MediaType.APPLICATION_JSON))

    val testKoeyringIkkjeStarta =
        TestKoeyring.from(
            TestConstants.crawlResultat.loeysing,
            URI(statusURL).toURL(),
            Brukar("test", "testar"),
            TestConstants.crawlResultat.antallNettsider)
    val response = autoTesterClient.updateStatus(testKoeyringIkkjeStarta)

    assertThat(response.isFailure).isTrue
  }

  private val jsonFailure =
      """{"runtimeStatus":"Completed", "output":[{
    "suksesskriterium": [
      "2.4.2"
    ],
    "side": "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
    "maalingId": 46,
    "loeysingId": 1,
    "testregelId": "QW-ACT-R1",
    "sideNivaa": 1,
    "testVartUtfoert": "3/23/2023, 11:15:54 AM"
  }]}"""
          .trimIndent()

  @DisplayName("Aggregering skal returnere null-verdiar i json fil")
  @Test()
  fun aggregeringReturnNullValues() {
    val jsonResult =
        """[{
            "fleireSuksesskriterium": [
            "2.5.3"
            ],
            "loeysing": {
            "id": 2429,
            "url": "https://www.jarlsberg-ikt.no/",
            "orgnummer": "919431016",
            "namn": "JARLSBERG IKT - INTERKOMMUNALT SAMARBEID"
        },
            "maalingId": 280,
            "suksesskriterium": "2.5.3",
            "talElementBrot": 0,
            "talElementSamsvar": 0,
            "talElementVarsel": 0,
            "talElementIkkjeForekomst": 0,
            "talSiderBrot": 0,
            "talSiderIkkjeForekomst": 10,
            "talSiderSamsvar": 0,
            "testregelId": "QW-ACT-R30",
            "testregelGjennomsnittlegSideBrotProsent": null,
            "testregelGjennomsnittlegSideSamsvarProsent": null
        }]"""
            .trimIndent()

    val lenker =
        AutoTesterClient.AutoTesterLenker(
            URI("https://fullt.resultat").toURL(),
            URI("https://brot.resultat").toURL(),
            URI("https://aggregering.resultat").toURL(),
            URI("https://aggregeringSK.resultat").toURL(),
            URI("https://aggregeringSide.resultat").toURL(),
            URI("https://aggregeringSideTR.resultat").toURL(),
            URI("https://aggregeringLoeysing.resultat").toURL(),
        )
    val testKoeyring =
        TestKoeyring.Ferdig(
            TestConstants.crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            lenker = lenker,
            Brukar("test", "testar"))

    server
        .expect(
            ExpectedCount.manyTimes(),
            MockRestRequestMatchers.requestTo(
                CoreMatchers.startsWith("https://aggregering.resultat")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(jsonResult, MediaType.APPLICATION_JSON))

    val response: AggregertResultatTestregel =
        runBlocking {
              autoTesterClient.fetchResultat(
                  listOf(testKoeyring), AutoTesterClient.ResultatUrls.urlAggreggeringTR)
            }
            .values
            .first()
            .getOrThrow()
            .first() as AggregertResultatTestregel

    assertThat(response.testregelGjennomsnittlegSideBrotProsent).isNull()
    assertThat(response.testregelGjennomsnittlegSideSamsvarProsent).isNull()
  }
}
