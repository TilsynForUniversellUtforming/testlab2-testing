package no.uutilsynet.testlab2testing.forenkletkontroll

import java.time.Instant

data class MaalingDbRow(
    val id: Int,
    val navn: String,
    val datoStart: Instant,
    val status: MaalingStatus,
    val maxLenker: Int,
    val talLenker: Int
)
