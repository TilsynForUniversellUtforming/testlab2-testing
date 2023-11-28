package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelType

open class RegelsettBase(
    open val id: Int,
    open val namn: String,
    open val type: TestregelType,
    open val standard: Boolean,
)
