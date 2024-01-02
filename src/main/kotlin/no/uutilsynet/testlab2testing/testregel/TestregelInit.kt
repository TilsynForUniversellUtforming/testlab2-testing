package no.uutilsynet.testlab2testing.testregel

data class TestregelInit(
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType
)

data class TestregelInitAutomatisk(
    val testregelId: String,
    val namn: String,
    val krav: String,
    val tema: Int,
    val testobjekt: Int
)

data class TestregelInitManuell(
    val testregelId: String,
    val namn: String,
    val tema: Int,
    val testobjekt: Int,
    val krav: String,
    val testregelSchema: String,
    val type: TestregelType,
    val status: TestregelStatus,
    val versjon: Int,
    val innholdstype: Int,
)
