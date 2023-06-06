package no.uutilsynet.testlab2testing.dto

data class Testregel(
    val id: Int,
    val krav: String,
    val testregelNoekkel: String,
    val kravTilSamsvar: String,
)
