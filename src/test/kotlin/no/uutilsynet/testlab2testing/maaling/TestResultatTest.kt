package no.uutilsynet.testlab2testing.maaling

import java.time.Month
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TestResultatTest {
  @Test
  @DisplayName("\"3/23/2023, 11:15:54 AM\" should parse")
  fun parseLocalDateTime() {
    val s = "3/23/2023, 11:15:54 AM"
    val result = TestResultat.parseLocalDateTime(s)
    assertThat(result.month).isEqualTo(Month.MARCH)
    assertThat(result.dayOfMonth).isEqualTo(23)
    assertThat(result.year).isEqualTo(2023)
    assertThat(result.hour).isEqualTo(11)
    assertThat(result.minute).isEqualTo(15)
    assertThat(result.second).isEqualTo(54)
  }
}
