package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import no.uutilsynet.testlab2testing.maaling.TestConstants.statusURL
import no.uutilsynet.testlab2testing.maaling.TestConstants.testRegelList
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
          objectMapper.readValue(jsonString, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(pending).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Pending::class.java)
    }

    @DisplayName("når responsen fra autotester er `Running`, så skal det parses til responsklassen")
    @Test
    fun running() {
      val jsonString =
          """{"runtimeStatus":"Running", "output": null, "customStatus":{"testaSider":0, "talSider":0}}"""
      val running =
          objectMapper.readValue(jsonString, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(running).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Running::class.java)

      val jsonStringNoStatus = """{"runtimeStatus":"Running", "output": null}"""
      val runningNoStatus =
          objectMapper.readValue(
              jsonStringNoStatus, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(runningNoStatus)
          .isInstanceOf(AutoTesterClient.AzureFunctionResponse.Running::class.java)
    }

    @DisplayName(
        "når responsen fra autotester er `Completed`, og responsen inneholder testresultater, så skal det parses til responsklassen")
    @Test
    fun completed() {
      val completed =
          objectMapper.readValue(jsonSuccess, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(completed)
          .isInstanceOf(AutoTesterClient.AzureFunctionResponse.Completed::class.java)
      val output: AutoTesterClient.AutoTesterOutput.TestResultater =
          (completed as AutoTesterClient.AzureFunctionResponse.Completed).output
              as AutoTesterClient.AutoTesterOutput.TestResultater
      assertThat(output.testResultater).hasSize(2)
    }

    @DisplayName(
        "når responsen fra autotester er `Completed`, og responsen inneholder urler til testresultater, så skal det parses til responsklassen")
    @Test
    fun completedWithURLs() {
      val jsonString =
          """{"runtimeStatus":"Completed", "output": {"urlFulltResultat": "https://fullt.resultat.no", "urlBrot": "https://brot.resultat.no","urlAggregeringTR": "https://aggregeringTR.resultat.no","urlAggregeringSK": "https://aggregeringSK.resultat.no","urlAggergeringSide": "https://aggregeringSide.resultat.no"}}"""
      val completed =
          objectMapper.readValue(jsonString, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(completed)
          .isInstanceOf(AutoTesterClient.AzureFunctionResponse.Completed::class.java)
      val output: AutoTesterClient.AutoTesterOutput.Lenker =
          (completed as AutoTesterClient.AzureFunctionResponse.Completed).output
              as AutoTesterClient.AutoTesterOutput.Lenker
      assertThat(output.urlFulltResultat).isEqualTo(URI("https://fullt.resultat.no").toURL())
      assertThat(output.urlBrot).isEqualTo(URI("https://brot.resultat.no").toURL())
      assertThat(output.urlAggregeringTR)
          .isEqualTo(java.net.URI("https://aggregeringTR.resultat.no").toURL())
    }

    @DisplayName("når responsen fra autotester er `Failed`, så skal det parses til responsklassen")
    @Test
    fun failed() {
      val jsonString = """{"runtimeStatus":"Failed", "output": "401 Unauthorized"}"""
      val failed =
          objectMapper.readValue(jsonString, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(failed).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Failed::class.java)
      assertThat((failed as AutoTesterClient.AzureFunctionResponse.Failed).output)
          .isEqualTo("401 Unauthorized")
    }

    @DisplayName("når responsen fra autotester er `Terminated`, så skal det kunne parses")
    @Test
    fun terminated() {
      val outputFromAutotester = """{"runtimeStatus":"Terminated", "output": null}"""
      val terminated =
          objectMapper.readValue(
              outputFromAutotester, AutoTesterClient.AzureFunctionResponse::class.java)
      assertThat(terminated)
          .isInstanceOf(AutoTesterClient.AzureFunctionResponse.Terminated::class.java)
    }
  }

  @DisplayName("Når man starter testing, skal den returnere statusQueryGetUri")
  @Test
  fun startTesting() {
    val maalingId = 1
    val crawlResultat = TestConstants.crawlResultat
    val expectedRequestData =
        mapOf(
            "urls" to crawlResultat.nettsider,
            "idMaaling" to maalingId,
            "idLoeysing" to crawlResultat.loeysing.id,
            "resultatSomFil" to true,
            "actRegler" to testRegelList.map { it.testregelNoekkel },
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

    val result = autoTesterClient.startTesting(maalingId, crawlResultat, testRegelList)

    assertThat(result.isSuccess).isTrue
    assertThat(result.getOrNull()).isEqualTo(statusUris.statusQueryGetUri.toURL())
  }

  @DisplayName(
      "Hvis det er korrekt respons fra autotester skal man oppdatere til riktig status for testkoeyring")
  @Test
  fun updateStatus() {
    server
        .expect(
            ExpectedCount.manyTimes(),
            MockRestRequestMatchers.requestTo(CoreMatchers.startsWith(statusURL)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(MockRestResponseCreators.withSuccess(jsonSuccess, MediaType.APPLICATION_JSON))

    val testKoeyringIkkjeStarta =
        TestKoeyring.from(TestConstants.crawlResultat, URI(statusURL).toURL())
    val response = autoTesterClient.updateStatus(testKoeyringIkkjeStarta)

    assertThat(response.isSuccess).isTrue
    assertThat(response.getOrNull()).isInstanceOf(TestKoeyring.Ferdig::class.java)
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
        TestKoeyring.from(TestConstants.crawlResultat, java.net.URI(statusURL).toURL())
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

  private val jsonSuccess =
      """{"runtimeStatus":"Completed", "output":[{
    "suksesskriterium": [
      "2.4.2"
    ],
    "side": "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
    "maalingId": 46,
    "loeysingId": 1,
    "testregelId": "QW-ACT-R1",
    "sideNivaa": 1,
    "testVartUtfoert": "3/23/2023, 11:15:54 AM",
    "elementUtfall": "The `title` element exists and it's not empty ('').",
    "elementResultat": "samsvar",
    "elementOmtale": [
      {
        "pointer": "html > head:nth-child(1) > title:nth-child(18)",
        "htmlCode": "PHRpdGxlPkRpZ2l0YWxlIGJhcnJpZXJhciB8IFRpbHN5bmV0IGZvciB1bml2ZXJzZWxsIHV0Zm9ybWluZyBhdiBpa3Q8L3RpdGxlPg=="
      }
    ]
  },
  {
    "suksesskriterium": [
      "3.1.1"
    ],
    "side": "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
    "maalingId": 46,
    "loeysingId": 1,
    "testregelId": "QW-ACT-R2",
    "sideNivaa": 1,
    "testVartUtfoert": "3/23/2023, 11:15:54 AM",
    "elementUtfall": "The `lang` attribute exists and has a value.",
    "elementResultat": "samsvar",
    "elementOmtale": [
      {
        "pointer": "html",
        "htmlCode": "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT48L2JvZHk+PC9odA=="
      }
    ]
  }]}"""
          .trimIndent()

  private val jsonTerminated = """{"runtimeStatus":"Terminated"}"""
}
