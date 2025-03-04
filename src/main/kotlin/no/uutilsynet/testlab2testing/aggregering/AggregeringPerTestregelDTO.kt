package no.uutilsynet.testlab2testing.aggregering

data class AggregeringPerTestregelDTO(
    val maalingId: Int?,
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
    val testregelGjennomsnittlegSideBrotProsent: Double?,
    val testgrunnlagId: Int?
)
