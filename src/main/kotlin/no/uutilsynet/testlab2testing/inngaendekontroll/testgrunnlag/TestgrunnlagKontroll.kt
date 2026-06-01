package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2testing.kontroll.Sideutval

data class TestgrunnlagKontroll(
    val id: Int,
    val kontrollId: Int,
    val namn: String,
    val testreglar: List<Int> = emptyList(),
    val sideutval: List<Sideutval> = emptyList(),
    val type: TestgrunnlagType,
    val datoOppretta: Instant
)

data class TestgrunnlagList(
    val opprinneligTest: TestgrunnlagKontroll,
    val restestar: List<TestgrunnlagKontroll>
) {
    fun toList(): List<TestgrunnlagKontroll> = listOf(opprinneligTest) + restestar
}

fun List<TestgrunnlagKontroll>.filterForLoeysing(loeysingId: Int): List<TestgrunnlagKontroll> =
    filter { it.sideutval.any { sideutval -> sideutval.loeysingId == loeysingId } }

fun List<TestgrunnlagKontroll>.newestTestgrunnlagIds(): Set<Int> {
    val loeysingIds = flatMap { it.sideutval }.map { it.loeysingId }.distinct()
    return loeysingIds.mapNotNull { loeysingId ->
        filterForLoeysing(loeysingId).maxByOrNull { it.id }?.id
    }.toSet()
}

