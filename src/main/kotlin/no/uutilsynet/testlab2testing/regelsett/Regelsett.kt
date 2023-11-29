package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelType

data class Regelsett(
    override val id: Int,
    override val namn: String,
    override val type: TestregelType,
    override val standard: Boolean,
    val testregelList: List<Testregel>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
