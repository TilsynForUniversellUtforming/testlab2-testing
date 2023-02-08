package no.uutilsynet.testlab2testing.dto

import java.time.LocalDate

data class Maaling(
    val id: Int,
    val namn: String,
    val idLoeysing: Int,
    val idSak: Int,
    val datoStart: LocalDate,
    val datoSlutt: LocalDate
)
