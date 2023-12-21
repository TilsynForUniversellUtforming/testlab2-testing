package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.loeysing.Loeysing

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
