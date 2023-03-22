package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TestKoeyringTest {
  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual = TestKoeyring.from(uutilsynetLoeysing, URL("https://status.url"))
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
    assertThat((actual as TestKoeyring.IkkjeStarta).statusURL.toString())
        .isEqualTo("https://status.url")
  }

  @DisplayName("når responsen er `Pending`, så blir statusen `Ikkje starta`")
  @Test
  fun pending() {
    val response = AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Pending)
    val testKoeyring =
        TestKoeyring.IkkjeStarta(uutilsynetLoeysing, Instant.now(), URL("http://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
  }

  @DisplayName("når responsen er `Running`, så blir statusen `Starta`")
  @Test
  fun running() {
    val response = AutoTesterClient.Response(AutoTesterClient.RuntimeStatus.Running)
    val testKoeyring =
        TestKoeyring.IkkjeStarta(uutilsynetLoeysing, Instant.now(), URL("https://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Starta::class.java)
  }
}
