package no.uutilsynet.testlab2testing.dto

data class Testregel(
    val id: Int,
    val kravId: Int,
    val referanseAct: String,
    val kravTilSamsvar: String,
    val type: String
)
