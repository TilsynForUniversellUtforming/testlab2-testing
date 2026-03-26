package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.Valid

/**
 * 
 * @param id 
 * @param testrunUuid 
 * @param loeysingId 
 * @param kravId 
 * @param talSiderSamsvar 
 * @param talSiderBrot 
 * @param talSiderIkkjeForekomst 
 */
data class TestresultatAggregertPerSuksesskriteriumRequestBody(

    @get:JsonProperty("id") val id: kotlin.Long? = null,

    @get:JsonProperty("testrunUuid") val testrunUuid: kotlin.String? = null,

    @get:JsonProperty("loeysingId") val loeysingId: kotlin.Int? = null,

    @get:JsonProperty("kravId") val kravId: kotlin.Int? = null,

    @get:JsonProperty("talSiderSamsvar") val talSiderSamsvar: kotlin.Int? = null,

    @get:JsonProperty("talSiderBrot") val talSiderBrot: kotlin.Int? = null,

    @get:JsonProperty("talSiderIkkjeForekomst") val talSiderIkkjeForekomst: kotlin.Int? = null
) {

}

