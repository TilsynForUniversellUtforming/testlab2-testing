package no.uutilsynet.testlab2testing.aggregering.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.util.UUID

@Suppress("LongParameterList")
@Entity(name = "aggregering_testregel")
class AggregeringPerTestregelEntity(
    var maalingId: Int?,
    var testgrunnlagId: Int?,
    @Column(columnDefinition = "testrun_uuid")
    var testrunUuid: UUID?,
    val loeysingId: Int,
    val testregelId: Int,
    val suksesskriterium: Int,
    val fleire_suksesskriterium: List<Int>,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val talElementVarsel: Int,
    val talSiderSamsvar: Int,
    val talSiderBrot: Int,
    val talSiderIkkjeForekomst: Int,
    val testregelGjennomsnittlegSideBrotProsent: Double?,
    val testregelGjennomsnittlegSideSamsvarProsent: Double?,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null
)
