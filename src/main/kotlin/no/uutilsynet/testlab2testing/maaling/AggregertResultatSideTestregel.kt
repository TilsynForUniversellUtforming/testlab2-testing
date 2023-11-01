package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatSideTestregel(
    val maalingId: Int,
    val testregelId: String,
    val loeysing: Loeysing,
    val sideUrl: String,
    val sideNivaa: Int,
    val bruddprosentTestregel: Int,
    val talElementSamvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int
) : AutotesterTestresultat
