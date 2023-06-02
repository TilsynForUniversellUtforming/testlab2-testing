package no.uutilsynet.testlab2testing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ThrowableExtraKtTest {

  @Test
  @DisplayName(
      "når vi henter firstMessage for en Throwable, så skal vi få meldingen for den innerste")
  fun testFirstMessage() {
    val throwable = Throwable("Outer", Throwable("Inner", Throwable("Innermost")))
    assertThat(throwable.firstMessage()).isEqualTo("Innermost")
  }

  @Test
  @DisplayName(
      "når vi henter firstMessage for en Throwable uten cause, så skal vi få meldingen for den gitte Throwable")
  fun testFirstMessageNoCause() {
    val throwable = Throwable("Outer")
    assertThat(throwable.firstMessage()).isEqualTo("Outer")
  }
}
