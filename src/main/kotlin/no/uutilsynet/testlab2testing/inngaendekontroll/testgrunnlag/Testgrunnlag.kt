package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.inngaendekontroll.aktivitet.Aktivitet
import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.Testregel

data class Testgrunnlag(
    val id: Int,
    val sakId: Int,
    val testgrupperingId: Int?,
    val namn: String,
    val testregelList: List<Testregel>,
    val utvalId: Int,
    val loeysingList: List<Loeysing>,
    val type: TestgrunnlagType,
    val aktivitet: List<Aktivitet>?,
    val datoOppretta: Instant
) {
  enum class TestgrunnlagType {
    OPPRINNELEG_TEST,
    RETEST
  }
}
