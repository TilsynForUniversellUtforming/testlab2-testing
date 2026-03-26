package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.Link
import no.uutilsynet.testlab2testing.resultat.external.model.PageMetadata
import no.uutilsynet.testlab2testing.resultat.external.model.PagedModelEntityModelTestgrunnlagEmbedded
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
 * @param page 
 */
data class PagedModelEntityModelTestgrunnlag(

    @field:Valid
    @get:JsonProperty("_embedded") val embedded: PagedModelEntityModelTestgrunnlagEmbedded? = null,

    @field:Valid
    @get:JsonProperty("_links") val links: kotlin.collections.Map<kotlin.String, Link>? = null,

    @field:Valid
    @get:JsonProperty("page") val page: PageMetadata? = null
) {

}

