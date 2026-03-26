package no.uutilsynet.testlab2testing.aggregering.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Suppress("LongParameterList")
@Entity(name = "aggregering_side")
class AggregeringPerSideEntity(
    var maalingId: Int?,
    var testgrunnlagId: Int?,
    var testrunUuid: String?,
    val loeysingId: Int,
    val side: String,
    val gjennomsnittligBruddProsentTr: Double?,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementIkkjeForekomst: Int,
    val talElementVarsel: Int,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null
)
