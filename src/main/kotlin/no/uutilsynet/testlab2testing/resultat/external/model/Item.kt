package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.AbstractJsonSchemaPropertyObject
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
 * @param type 
 * @param properties 
 * @param requiredProperties 
 */
data class Item(

    @get:JsonProperty("type") val type: kotlin.String? = null,

    @field:Valid
    @get:JsonProperty("properties") val properties: kotlin.collections.Map<kotlin.String, AbstractJsonSchemaPropertyObject>? = null,

    @get:JsonProperty("requiredProperties") val requiredProperties: kotlin.collections.List<kotlin.String>? = null
) {

}

