package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.loeysing.Loeysing

data class AggregertResultatTestregel(
    var maalingId: Int,
    val loeysing: Loeysing,
    var testregelId: String,
    val suksesskriterium: String,
    val fleireSuksesskriterium: List<String>,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testregelGjennomsnittlegSideSamsvarProsent: Float?,
    var testregelGjennomsnittlegSideBrotProsent: Float?,
) : AutotesterTestresultat
