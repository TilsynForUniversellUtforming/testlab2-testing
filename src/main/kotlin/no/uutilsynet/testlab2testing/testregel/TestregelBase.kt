package no.uutilsynet.testlab2testing.testregel

open class TestregelBase(
    open val id: Int,
    open val namn: String,
    open val kravId: Int,
    open val modus: TestregelModus,
)
