package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelModus

data class RegelsettEdit(
    override val id: Int,
    override val namn: String,
    override val modus: TestregelModus,
    override val standard: Boolean,
    val testregelIdList: List<Int>,
) :
    RegelsettBase(
        id,
        namn,
        modus,
        standard,
    )
