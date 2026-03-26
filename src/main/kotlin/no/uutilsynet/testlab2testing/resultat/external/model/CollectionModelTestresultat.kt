package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.CollectionModelTestresultatEmbedded
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
 * @param embedded 
 * @param links 
 */
data class CollectionModelTestresultat(

    @field:Valid
    @get:JsonProperty("_embedded") val embedded: CollectionModelTestresultatEmbedded? = null,

    @field:Valid
    @get:JsonProperty("_links") val links: kotlin.collections.Map<kotlin.String, Link>? = null
) {

}

