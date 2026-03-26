package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.Link
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
 * @param testrunUuid 
 * @param loeysingId 
 * @param kravId 
 * @param talSiderSamsvar 
 * @param talSiderBrot 
 * @param talSiderIkkjeForekomst 
 * @param links 
 */
data class EntityModelTestresultatAggregertPerSuksesskriterium(

    @get:JsonProperty("testrunUuid") val testrunUuid: kotlin.String? = null,

    @get:JsonProperty("loeysingId") val loeysingId: kotlin.Int? = null,

    @get:JsonProperty("kravId") val kravId: kotlin.Int? = null,

    @get:JsonProperty("talSiderSamsvar") val talSiderSamsvar: kotlin.Int? = null,

    @get:JsonProperty("talSiderBrot") val talSiderBrot: kotlin.Int? = null,

    @get:JsonProperty("talSiderIkkjeForekomst") val talSiderIkkjeForekomst: kotlin.Int? = null,

    @field:Valid
    @get:JsonProperty("_links") val links: kotlin.collections.Map<kotlin.String, Link>? = null
) {

}

