package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelResponse
import no.uutilsynet.testlab2testing.testregel.TestregelType

data class RegelsettResponse(
    override val id: Int,
    override val namn: String,
    override val type: TestregelType,
    override val standard: Boolean,
    val testregelList: List<TestregelResponse>,
) :
    RegelsettBase(
        id,
        namn,
        type,
        standard,
    )
