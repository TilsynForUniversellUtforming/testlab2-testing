package no.uutilsynet.testlab2testing.testresultat.aggregering

data class AggregeringPerSuksesskriteriumDB(
    val maalingId: Int?,
    val loeysingId: Int,
    val suksesskriteriumId: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testgrunnlagId: Int?
) {}
