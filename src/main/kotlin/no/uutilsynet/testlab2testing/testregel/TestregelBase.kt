package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus

open class TestregelBase(
    open val id: Int,
    open val namn: String,
    open val kravId: Int,
    open val modus: TestregelModus,
    open val type: TestregelInnholdstype
)
