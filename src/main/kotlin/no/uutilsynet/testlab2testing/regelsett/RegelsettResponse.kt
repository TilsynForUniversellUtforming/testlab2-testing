package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelDTO
import no.uutilsynet.testlab2testing.testregel.TestregelModus

data class RegelsettResponse(
    override val id: Int,
    override val namn: String,
    override val type: TestregelModus,
    override val standard: Boolean,
    val testregelList: List<TestregelDTO>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
