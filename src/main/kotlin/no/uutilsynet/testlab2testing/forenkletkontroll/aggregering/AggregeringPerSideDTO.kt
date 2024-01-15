package no.uutilsynet.testlab2testing.forenkletkontroll.aggregering

import java.net.URL

data class AggregeringPerSideDTO(
    val maalingId: Int,
    val loeysingId: Int,
    val sideUrl: URL,
    val sideNivaa: Int,
    val gjennomsnittligBruddProsentTR: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int
)
