package no.uutilsynet.testlab2testing.dto

data class Test(
    val id: Int,
    val testresultat: String,
    val testresultatBeskrivelse: String,
    val side: String,
    val elementbeskrivelse: String,
    val elementHtmlkode: String,
    val elementPeikar: String,
    val idTestregel: Int,
    val idMaaling: Int
)
