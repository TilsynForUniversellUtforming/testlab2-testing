package no.uutilsynet.testlab2testing.testregel.model

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x

data class TestregelAggregate(
    val id: Int,
    val testregelId: String,
    val namn: String,
    val krav: KravWcag2x,
    val modus: TestregelModus,
    val tema: Tema?,
)
