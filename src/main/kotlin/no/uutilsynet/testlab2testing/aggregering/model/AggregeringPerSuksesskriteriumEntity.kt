package no.uutilsynet.testlab2testing.aggregering.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Suppress("LongParameterList")
@Entity(name = "aggregering_suksesskriterium")
class AggregeringPerSuksesskriteriumEntity(
    var maalingId: Int?,
    var testgrunnlagId: Int?,
    var testrunUuid: String?,
    val loeysingId: Int,
    val suksesskriteriumId: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    @Id @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY) var id: Int? = null
)
