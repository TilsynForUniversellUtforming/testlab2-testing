package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.EntityModelTestresultatAggregertPerTestregel
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
 * @param testresultatAggregertPerTestregel 
 */
data class PagedModelEntityModelTestresultatAggregertPerTestregelEmbedded(

    @field:Valid
    @get:JsonProperty("TestresultatAggregertPerTestregel") val testresultatAggregertPerTestregel: kotlin.collections.List<EntityModelTestresultatAggregertPerTestregel>? = null
) {

}

