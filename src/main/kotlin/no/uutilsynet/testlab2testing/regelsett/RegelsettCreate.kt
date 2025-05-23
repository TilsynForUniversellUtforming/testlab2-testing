package no.uutilsynet.testlab2testing.regelsett

import no.uutilsynet.testlab2.constants.TestregelModus

data class RegelsettCreate(
    val namn: String,
    val modus: TestregelModus,
    val standard: Boolean,
    val testregelIdList: List<Int>,
)
