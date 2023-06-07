package no.uutilsynet.testlab2testing.dto

data class TestregelDTO(
    val id: Int,
    val kravId: Int?,
    val testregelNoekkel: String?,
    val kravTilSamsvar: String,
    val type: String,
    val status: String
)
