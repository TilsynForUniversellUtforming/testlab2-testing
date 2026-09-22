package no.uutilsynet.testlab2testing.loeysing.utval

import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class Utval(
    val id: Int,
    val namn: String,
    val loeysingar: List<Loeysing>,
    val oppretta: Instant
)

data class UtvalListItem(val id: UtvalId, val namn: String, val oppretta: Instant)
