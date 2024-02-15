package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelModus

data class RegelsettResponse(
    override val id: Int,
    override val namn: String,
    override val type: TestregelModus,
    override val standard: Boolean,
    val testregelList: List<TestregelBase>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
