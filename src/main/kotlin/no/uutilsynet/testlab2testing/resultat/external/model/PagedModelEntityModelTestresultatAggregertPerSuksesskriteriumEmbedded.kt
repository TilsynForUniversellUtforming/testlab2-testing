package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.EntityModelTestresultatAggregertPerSuksesskriterium
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
 * @param testresultatAggregertPerSuksesskriterium 
 */
data class PagedModelEntityModelTestresultatAggregertPerSuksesskriteriumEmbedded(

    @field:Valid
    @get:JsonProperty("TestresultatAggregertPerSuksesskriterium") val testresultatAggregertPerSuksesskriterium: kotlin.collections.List<EntityModelTestresultatAggregertPerSuksesskriterium>? = null
) {

}

