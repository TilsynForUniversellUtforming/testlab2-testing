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
import org.junit.jupiter.params.provider.ValueSource

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
  fun testUpdateStatus(response: AutoTesterClient.Response, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.IkkjeStarta(uutilsynetLoeysing, Instant.now(), URL("http://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatusFromStarta(response: AutoTesterClient.Response, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.Starta(uutilsynetLoeysing, Instant.now(), URL("http://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `ferdig`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @ValueSource(strings = ["Pending", "Running", "Completed", "Failed"])
  fun testUpdateStatusFromFerdig(response: String) {
    val testKoeyring = TestKoeyring.Ferdig(uutilsynetLoeysing, Instant.now())
    val actual =
        TestKoeyring.updateStatus(
            testKoeyring,
            AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.valueOf(response)))
    assertThat(actual).isInstanceOf(TestKoeyring.Ferdig::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `feila`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @ValueSource(strings = ["Pending", "Running", "Completed", "Failed"])
  fun testUpdateStatusFromFeila(response: String) {
    val testKoeyring = TestKoeyring.Feila(uutilsynetLoeysing, Instant.now(), "dette går ikkje")
    val actual =
        TestKoeyring.updateStatus(
            testKoeyring,
            AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.valueOf(response)))
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  companion object {
    @JvmStatic
    fun pairsOfResponseTilstand(): Stream<Arguments> {
      return Stream.of(
          Arguments.of(
              AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Pending),
              TestKoeyring.IkkjeStarta::class.java),
          Arguments.of(
              AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Running),
              TestKoeyring.Starta::class.java),
          Arguments.of(
              AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Completed),
              TestKoeyring.Ferdig::class.java),
          Arguments.of(
              AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Failed),
              TestKoeyring.Feila::class.java))
    }
  }
}
