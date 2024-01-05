package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelModus

data class Regelsett(
    override val id: Int,
    override val namn: String,
    override val type: TestregelModus,
    override val standard: Boolean,
    val testregelList: List<Testregel>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
