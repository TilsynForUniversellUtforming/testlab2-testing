package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.aktivitet.Aktivitet
import no.uutilsynet.testlab2testing.kontroll.SideutvalElement
import no.uutilsynet.testlab2testing.testregel.Testregel

data class TestgrunnlagKontroll(
    val id: Int,
    val kontrollId: Int,
    val namn: String,
    val testreglar: List<Testregel> = emptyList(),
    val sideutval: List<SideutvalElement> = emptyList(),
    val type: TestgrunnlagType,
    val aktivitet: List<Aktivitet>?,
    val datoOppretta: Instant
)

data class TestgrunnlagList(
    val opprinneligTest: TestgrunnlagKontroll,
    val restestar: List<TestgrunnlagKontroll>
)
