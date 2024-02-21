package no.uutilsynet.testlab2testing.testregel

open class TestregelBase(
    open val id: Int,
    open val namn: String,
    open val krav: String,
    open val modus: TestregelModus,
)
