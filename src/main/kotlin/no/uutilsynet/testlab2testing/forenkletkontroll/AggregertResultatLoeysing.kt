package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatLoeysing(
    val maalingId: Int,
    val loeysing: Loeysing,
    val gjennomsnittligBruddprosentTR: Int,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int
) : AutotesterTestresultat
