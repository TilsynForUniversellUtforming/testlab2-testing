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
 * @param href 
 * @param hreflang 
 * @param title 
 * @param type 
 * @param deprecation 
 * @param profile 
 * @param name 
 * @param templated 
 */
data class Link(

    @get:JsonProperty("href") val href: kotlin.String? = null,

    @get:JsonProperty("hreflang") val hreflang: kotlin.String? = null,

    @get:JsonProperty("title") val title: kotlin.String? = null,

    @get:JsonProperty("type") val type: kotlin.String? = null,

    @get:JsonProperty("deprecation") val deprecation: kotlin.String? = null,

    @get:JsonProperty("profile") val profile: kotlin.String? = null,

    @get:JsonProperty("name") val name: kotlin.String? = null,

    @get:JsonProperty("templated") val templated: kotlin.Boolean? = null
) {

}

