package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2testing.testregel.TestregelBase

data class RegelsettResponse(
    override val id: Int,
    override val namn: String,
    override val modus: TestregelModus,
    override val standard: Boolean,
    val testregelList: List<TestregelBase>,
) :
    RegelsettBase(
        id,
        namn,
        modus,
        standard,
    )
