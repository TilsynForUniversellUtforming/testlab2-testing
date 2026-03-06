package no.uutilsynet.testlab2testing.testresultat.aggregering

data class AggregeringPerTestregelExport(
    val testrunUuid: String,
    val loeysingId: Int,
    val testregelId: Int,
    val suksesskriterium: Int,
    val fleireSuksesskriterium: List<Int>,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testregelGjennomsnittlegSideSamsvarProsent: Double?,
    val testregelGjennomsnittlegSideBrotProsent: Double?
)