package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatSuksesskriterium(
    val maalingId: Int,
    val loeysing: Loeysing,
    val suksesskriterium: String,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int
) : AutotesterTestresultat
