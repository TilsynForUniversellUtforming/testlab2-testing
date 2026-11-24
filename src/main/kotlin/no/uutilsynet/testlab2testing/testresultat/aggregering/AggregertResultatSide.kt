package no.uutilsynet.testlab2testing.testresultat.aggregering

import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testing.automatisk.AutotesterTestresultat

data class AggregertResultatSide(
    val maalingId: Int?,
    val loeysing: Loeysing,
    val sideUrl: String,
    val sideNivaa: Int,
    val gjennomsnittligBruddProsentTR: Double?,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int
) : AutotesterTestresultat
