package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonCreator
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class TestResultat(
    val suksesskriterium: List<String>,
    val side: URL,
    val testregelId: String,
    val sideNivaa: Int,
    val testVartUtfoert: LocalDateTime,
    val elementUtfall: String,
    val elementResultat: String,
    val elementOmtale: ACTElement?
) : AutotesterTestresultat {
  data class ACTElement(val htmlCode: String, val pointer: String)

  companion object {
    @JsonCreator
    @JvmStatic
    fun create(
        suksesskriterium: List<String>,
        side: URL,
        testregelId: String,
        sideNivaa: Int,
        testVartUtfoert: String,
        elementUtfall: String,
        elementResultat: String,
        elementOmtale: List<ACTElement>? = null
    ): TestResultat {
      return TestResultat(
          suksesskriterium,
          side,
          testregelId,
          sideNivaa,
          parseLocalDateTime(testVartUtfoert),
          elementUtfall,
          elementResultat,
          elementOmtale?.first())
    }

    fun parseLocalDateTime(s: String): LocalDateTime {
      val formatter = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a", Locale.ENGLISH)
      return LocalDateTime.parse(s, formatter)
    }
  }
}
