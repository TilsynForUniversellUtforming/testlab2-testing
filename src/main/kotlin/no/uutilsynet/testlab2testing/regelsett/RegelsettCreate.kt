package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelType

data class RegelsettCreate(
    val namn: String,
    val type: TestregelType,
    val standard: Boolean,
    val testregelIdList: List<Int>,
)
