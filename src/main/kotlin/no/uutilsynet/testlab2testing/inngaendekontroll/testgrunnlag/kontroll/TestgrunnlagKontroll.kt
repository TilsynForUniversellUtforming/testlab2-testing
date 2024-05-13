package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.aktivitet.Aktivitet
import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.testregel.Testregel

data class TestgrunnlagKontroll(
    val id: Int,
    val kontrollId: Int,
    val namn: String,
    val testreglar: List<Testregel> = emptyList(),
    val sideutval: List<Sideutval> = emptyList(),
    val type: TestgrunnlagType,
    val aktivitet: List<Aktivitet>?,
    val datoOppretta: Instant
) {
  enum class TestgrunnlagType {
    OPPRINNELEG_TEST,
    RETEST
  }
}
