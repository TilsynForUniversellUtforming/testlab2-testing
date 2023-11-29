package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelType

data class RegelsettEdit(
    override val id: Int,
    override val namn: String,
    override val type: TestregelType,
    override val standard: Boolean,
    val testregelIdList: List<Int>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
