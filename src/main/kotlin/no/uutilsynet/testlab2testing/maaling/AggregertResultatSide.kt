package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatSide(
    val maalingId: Int,
    val loeysing: Loeysing,
    val sideUrl: URL,
    val sideNivaa: Int,
    val gjennomsnittligBruddProsentTR: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int
) : AutotesterTestresultat
