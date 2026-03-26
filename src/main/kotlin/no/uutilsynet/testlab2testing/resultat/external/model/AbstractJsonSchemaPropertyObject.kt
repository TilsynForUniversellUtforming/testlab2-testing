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
 * @param title 
 * @param readOnly 
 */
data class AbstractJsonSchemaPropertyObject(

    @get:JsonProperty("title") val title: kotlin.String? = null,

    @get:JsonProperty("readOnly") val readOnly: kotlin.Boolean? = null
) {

}

