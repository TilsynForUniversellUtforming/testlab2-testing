package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2testing.testregel.TestregelModus

data class RegelsettCreate(
    val namn: String,
    val type: TestregelModus,
    val standard: Boolean,
    val testregelIdList: List<Int>,
)
