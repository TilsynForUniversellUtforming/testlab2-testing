package no.uutilsynet.testlab2testing.aggregering

import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testing.manuelltesting.AutotesterTestresultat

data class AggregertResultatSuksesskriterium(
    val maalingId: Int?,
    val loeysing: Loeysing,
    val suksesskriterium: String,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int
) : AutotesterTestresultat
