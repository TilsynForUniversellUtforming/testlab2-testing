package no.uutilsynet.testlab2testing.maaling

import java.net.URL
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
}
