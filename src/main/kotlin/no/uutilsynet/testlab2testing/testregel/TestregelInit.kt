package no.uutilsynet.testlab2testing.testregel

data class TestregelInit(
    val namn: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType,
    val testregelNoekkel: String,
    val status: TestregelStatus,
    val versjon: Int,
    val tema: Int,
    val inngoldstype: Int,
)
