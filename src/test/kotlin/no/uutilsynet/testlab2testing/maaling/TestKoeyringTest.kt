package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TestKoeyringTest {
  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual = TestKoeyring.from(uutilsynetLoeysing, URL("https://status.url"))
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
    assertThat((actual as TestKoeyring.IkkjeStarta).statusURL.toString())
        .isEqualTo("https://status.url")
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `ikkje starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatus(response: AutoTesterClient.AzureFunctionResponse, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.IkkjeStarta(uutilsynetLoeysing, Instant.now(), URL("http://status.url"))
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
        TestKoeyring.Starta(uutilsynetLoeysing, Instant.now(), URL("http://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
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
        TestKoeyring.Ferdig(uutilsynetLoeysing, Instant.now(), URL("https://status.url"))
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
    val testKoeyring = TestKoeyring.Feila(uutilsynetLoeysing, Instant.now(), "dette går ikkje")
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
              AutoTesterClient.AzureFunctionResponse.Running, TestKoeyring.Starta::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Completed(testResultater()),
              TestKoeyring.Ferdig::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Failed("401 Unauthorized"),
              TestKoeyring.Feila::class.java))
    }

    private fun testResultater() =
        listOf(
            AutoTesterClient.TestResultat(
                listOf("3.1.1"),
                "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
                46,
                1,
                "QW-ACT-R5",
                1,
                "3/23/2023 11:15:54 AM",
                "The `lang` attribute has a valid value.",
                "samsvar",
                listOf(
                    AutoTesterClient.ACTElement(
                        "html",
                        "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT53aW5kb3cuZGF0YQ=="))),
            AutoTesterClient.TestResultat(
                listOf("4.1.2"),
                "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
                46,
                1,
                "QW-ACT-R11",
                1,
                "3/23/2023 11:15:54 AM",
                "The test target has an accessible name.",
                "samsvar",
                listOf(
                    AutoTesterClient.ACTElement(
                        "html > body:nth-child(2) > div:nth-child(2) > div:nth-child(1) > header:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > button:nth-child(1)",
                        "PGJ1dHRvbiBjbGFzcz0iaGVhZGVyLWJ1dHRvbiBoZWFkZXItYnV0dG9uLS1zZWFyY2ggY29sbGFwc2VkIiBkYXRhLWJzLXRvZ2dsZT0iY29sbGFwc2UiIGRhdGEtYnMtdGFyZw=="))))
  }
}
