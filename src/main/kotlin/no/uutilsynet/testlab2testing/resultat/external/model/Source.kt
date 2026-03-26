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
 * @param productName 
 * @param productComponentName 
 * @param productVersion 
 */
data class Source(

    @get:JsonProperty("productName") val productName: kotlin.String? = null,

    @get:JsonProperty("productComponentName") val productComponentName: kotlin.String? = null,

    @get:JsonProperty("productVersion") val productVersion: kotlin.String? = null
) {

}

