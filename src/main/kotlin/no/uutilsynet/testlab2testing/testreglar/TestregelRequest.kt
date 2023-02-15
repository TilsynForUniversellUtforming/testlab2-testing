package no.uutilsynet.testlab2testing.testreglar

data class TestregelRequest(
    val kravId: Int?,
    val referanseAct: String?,
    val kravTilSamsvar: String,
    val type: String,
    val status: String,
)
