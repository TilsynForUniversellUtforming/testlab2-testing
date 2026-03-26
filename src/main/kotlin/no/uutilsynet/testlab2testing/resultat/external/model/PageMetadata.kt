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
 * @param propertySize 
 * @param totalElements 
 * @param totalPages 
 * @param number 
 */
data class PageMetadata(

    @get:JsonProperty("size") val propertySize: kotlin.Long? = null,

    @get:JsonProperty("totalElements") val totalElements: kotlin.Long? = null,

    @get:JsonProperty("totalPages") val totalPages: kotlin.Long? = null,

    @get:JsonProperty("number") val number: kotlin.Long? = null
) {

}

