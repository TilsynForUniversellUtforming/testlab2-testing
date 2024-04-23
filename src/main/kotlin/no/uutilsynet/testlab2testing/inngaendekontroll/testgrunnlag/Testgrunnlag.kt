package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.aktivitet.Aktivitet
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.testregel.Testregel

data class Testgrunnlag(
    val id: Int,
    val sakId: Int,
    val namn: String,
    val testreglar: List<Testregel> = emptyList(),
    val loeysingar: List<Sak.Loeysing> = emptyList(),
    val type: TestgrunnlagType,
    val aktivitet: List<Aktivitet>?,
    val datoOppretta: Instant
) {
  enum class TestgrunnlagType {
    OPPRINNELEG_TEST,
    RETEST
  }
}
