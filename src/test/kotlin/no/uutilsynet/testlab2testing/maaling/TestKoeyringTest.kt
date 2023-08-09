package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.Instant
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.maaling.TestConstants.crawlResultat
import no.uutilsynet.testlab2testing.maaling.TestConstants.statusURL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.net.URL

class TestKoeyringTest {

  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual = TestKoeyring.from(crawlResultat, URI(statusURL).toURL())
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
    assertThat((actual as TestKoeyring.IkkjeStarta).statusURL.toString()).isEqualTo(statusURL)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `ikkje starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatus(response: AutoTesterClient.AzureFunctionResponse, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.IkkjeStarta(crawlResultat, Instant.now(), URI(statusURL).toURL())
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatusFromStarta(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat,
            Instant.now(),
            URI(statusURL).toURL(),
            Framgang(0, crawlResultat.nettsider.size))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @DisplayName("når ny responsen er `Terminated`, så skal ny tilstand bli `feila`")
  @Test
  fun testUpdateStatusFromStartaTerminated() {
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat,
            Instant.now(),
            URI(statusURL).toURL(),
            Framgang(0, crawlResultat.nettsider.size))
    val actual =
        TestKoeyring.updateStatus(testKoeyring, AutoTesterClient.AzureFunctionResponse.Terminated)
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `ferdig`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFerdig(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring =
        TestKoeyring.Ferdig(crawlResultat, Instant.now(), URI(statusURL).toURL(), testResultater())
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Ferdig::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `feila`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFeila(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring = TestKoeyring.Feila(crawlResultat, Instant.now(), "dette går ikkje")
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  companion object {
    @JvmStatic
    fun pairsOfResponseTilstand(): Stream<Arguments> {
      return Stream.of(
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Pending, TestKoeyring.IkkjeStarta::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Running(AutoTesterClient.CustomStatus(0, 1)),
              TestKoeyring.Starta::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Completed(
                  AutoTesterClient.AutoTesterOutput.Lenker(
                      URI("https://fullt.resultat").toURL(), URI("https://brot.resultat").toURL(), URI("https://aggregering.resultat").toURL()
                  )),
              TestKoeyring.Ferdig::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Failed("401 Unauthorized"),
              TestKoeyring.Feila::class.java))
    }

    fun testResultater() =
        listOf(
            TestResultat(
                listOf("3.1.1"),
                URI("https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160")
                    .toURL(),
                "QW-ACT-R5",
                1,
                TestResultat.parseLocalDateTime("3/23/2023, 11:15:54 AM"),
                "The `lang` attribute has a valid value.",
                "samsvar",
                TestResultat.ACTElement(
                    "html",
                    "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT53aW5kb3cuZGF0YQ==")),
            TestResultat(
                listOf("4.1.2"),
                URI("https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160")
                    .toURL(),
                "QW-ACT-R11",
                1,
                TestResultat.parseLocalDateTime("3/23/2023, 11:15:54 AM"),
                "The test target has an accessible name.",
                "samsvar",
                TestResultat.ACTElement(
                    "html > body:nth-child(2) > div:nth-child(2) > div:nth-child(1) > header:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > button:nth-child(1)",
                    "PGJ1dHRvbiBjbGFzcz0iaGVhZGVyLWJ1dHRvbiBoZWFkZXItYnV0dG9uLS1zZWFyY2ggY29sbGFwc2VkIiBkYXRhLWJzLXRvZ2dsZT0iY29sbGFwc2UiIGRhdGEtYnMtdGFyZw==")))
  }
}
