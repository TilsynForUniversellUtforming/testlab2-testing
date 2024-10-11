package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.annotation.JsonCreator
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert

data class TestResultat(
    val suksesskriterium: List<String>,
    val side: URL,
    val testregelId: String,
    val loeysingId: Int,
    val sideNivaa: Int,
    val testVartUtfoert: LocalDateTime,
    val elementUtfall: String,
    val elementResultat: TestresultatUtfall,
    val elementOmtale: TestresultatDetaljert.ElementOmtale?
) : AutotesterTestresultat {

  companion object {
    @JsonCreator
    @JvmStatic
    fun create(
        suksesskriterium: List<String>,
        side: URL,
        testregelId: String,
        loeysingId: Int,
        sideNivaa: Int,
        testVartUtfoert: String,
        elementUtfall: String,
        elementResultat: TestresultatUtfall,
        elementOmtale: List<TestresultatDetaljert.ElementOmtale>? = null
    ): TestResultat {
      return TestResultat(
          suksesskriterium,
          side,
          testregelId,
          loeysingId,
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
