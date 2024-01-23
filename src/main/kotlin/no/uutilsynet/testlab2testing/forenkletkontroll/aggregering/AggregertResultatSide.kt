package no.uutilsynet.testlab2testing.forenkletkontroll.aggregering

import java.net.URL
import no.uutilsynet.testlab2testing.forenkletkontroll.AutotesterTestresultat
import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatSide(
    val maalingId: Int,
    val loeysing: Loeysing,
    val sideUrl: URL,
    val sideNivaa: Int,
    val gjennomsnittligBruddProsentTR: Float?,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int
) : AutotesterTestresultat
