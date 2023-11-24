package no.uutilsynet.testlab2testing.testregel

data class TestregelInit(
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType
)
