package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatTestregel(
    val maalingId: Int,
    val loeysing: Loeysing,
    val testregelId: String,
    val suksesskriterium: String,
    val fleireSuksesskriterium: List<String>,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testregelGjennomsnittlegScore: Float
) : AutotesterTestresultat
