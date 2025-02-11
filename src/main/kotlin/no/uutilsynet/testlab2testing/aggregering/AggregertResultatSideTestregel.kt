package no.uutilsynet.testlab2testing.aggregering

import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testing.automatisk.AutotesterTestresultat

data class AggregertResultatSideTestregel(
    val maalingId: Int,
    val testregelId: String,
    val loeysing: Loeysing,
    val sideUrl: String,
    val sideNivaa: Int,
    val bruddprosentTestregel: Float?,
    val talElementSamvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int
) : AutotesterTestresultat
