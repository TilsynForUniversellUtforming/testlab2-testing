package no.uutilsynet.testlab2testing.aggregering

data class AggregeringPerSuksesskriteriumDTO(
    val maalingId: Int?,
    val loeysingId: Int,
    val suksesskriteriumId: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testgrunnlagId: Int? = 0
) {}
