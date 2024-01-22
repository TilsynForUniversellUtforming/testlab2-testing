package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelModus

open class RegelsettBase(
    open val id: Int,
    open val namn: String,
    open val type: TestregelModus,
    open val standard: Boolean,
)
