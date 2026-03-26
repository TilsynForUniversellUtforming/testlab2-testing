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
 * @param testregelId 
 * @param loeysingId 
 * @param testregelGjennomsnittlegSideBrotProsent 
 * @param testregelGjennomsnittlegSideSamsvarProsent 
 * @param talElementSamsvar 
 * @param talElementBrot 
 * @param talElementVarsel 
 * @param talElementIkkjeForekomst 
 * @param talSiderSamsvar 
 * @param talSiderBrot 
 * @param talSiderIkkjeForekomst 
 * @param links 
 */
data class EntityModelTestresultatAggregertPerTestregel(

    @get:JsonProperty("testrunUuid") val testrunUuid: kotlin.String? = null,

    @get:JsonProperty("testregelId") val testregelId: kotlin.Int? = null,

    @get:JsonProperty("loeysingId") val loeysingId: kotlin.Int? = null,

    @get:JsonProperty("testregelGjennomsnittlegSideBrotProsent") val testregelGjennomsnittlegSideBrotProsent: kotlin.Double? = null,

    @get:JsonProperty("testregelGjennomsnittlegSideSamsvarProsent") val testregelGjennomsnittlegSideSamsvarProsent: kotlin.Double? = null,

    @get:JsonProperty("talElementSamsvar") val talElementSamsvar: kotlin.Int? = null,

    @get:JsonProperty("talElementBrot") val talElementBrot: kotlin.Int? = null,

    @get:JsonProperty("talElementVarsel") val talElementVarsel: kotlin.Int? = null,

    @get:JsonProperty("talElementIkkjeForekomst") val talElementIkkjeForekomst: kotlin.Int? = null,

    @get:JsonProperty("talSiderSamsvar") val talSiderSamsvar: kotlin.Int? = null,

    @get:JsonProperty("talSiderBrot") val talSiderBrot: kotlin.Int? = null,

    @get:JsonProperty("talSiderIkkjeForekomst") val talSiderIkkjeForekomst: kotlin.Int? = null,

    @field:Valid
    @get:JsonProperty("_links") val links: kotlin.collections.Map<kotlin.String, Link>? = null
) {

}

