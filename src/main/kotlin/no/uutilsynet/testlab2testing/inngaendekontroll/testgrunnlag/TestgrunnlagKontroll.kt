package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.testregel.Testregel
import java.time.Instant

data class TestgrunnlagKontroll(
    val id: Int,
    val kontrollId: Int,
    val namn: String,
    val testreglar: List<Testregel> = emptyList(),
    val sideutval: List<Sideutval> = emptyList(),
    val type: TestgrunnlagType,
    val datoOppretta: Instant
)

data class TestgrunnlagList(
    val opprinneligTest: TestgrunnlagKontroll,
    val restestar: List<TestgrunnlagKontroll>
)
