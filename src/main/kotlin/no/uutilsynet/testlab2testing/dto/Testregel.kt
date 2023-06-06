package no.uutilsynet.testlab2testing.dto

data class Testregel(
    val id: Int,
    val krav: String,
    val referanseAct: String,
    val kravTilSamsvar: String,
)
