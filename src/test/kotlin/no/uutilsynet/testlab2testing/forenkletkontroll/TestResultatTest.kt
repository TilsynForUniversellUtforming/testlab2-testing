package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Month
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
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

  @Test
  @DisplayName(
      "gitt et testresultat som ikke har elementOmtale, så skal vi kunne lese det som en instans av TestResultat")
  fun manglerElementOmtale() {
    val json =
        """
        {
          "timestamp": "2023-06-02T08:51:10.9691428Z",
          "maalingId": 120,
          "loeysingId": 24,
          "testregelId": "QW-ACT-R1",
          "suksesskriterium": [ "2.4.2" ],
          "side": "https://www.alver.kommune.no/aktuelt/nyheiter/gratulerer-med-nasjonaldagen",
          "sideNivaa": 3,
          "elementResultat": "brot",
          "elementUtfall": "The `title` element doesn't exist.",
          "testVartUtfoert": "6/2/2023, 8:51:10 AM"
        }
      """
            .trimIndent()
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val testResultat = objectMapper.readValue(json, TestResultat::class.java)
    assertThat(testResultat.elementOmtale).isNull()
  }
}
