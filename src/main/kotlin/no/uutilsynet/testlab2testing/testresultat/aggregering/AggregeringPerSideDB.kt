package no.uutilsynet.testlab2testing.testresultat.aggregering

data class AggregeringPerSideDB(
    val maalingId: Int?,
    val loeysingId: Int,
    val sideUrl: String,
    val sideNivaa: Int,
    val gjennomsnittligBruddProsentTR: Double?,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int,
    val testgrunnlagId: Int?
) {}
