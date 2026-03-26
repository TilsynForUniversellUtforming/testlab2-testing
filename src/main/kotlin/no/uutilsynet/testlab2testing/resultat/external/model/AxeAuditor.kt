package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.resultat.external.model.AllIssue
import no.uutilsynet.testlab2testing.resultat.external.model.Source
import no.uutilsynet.testlab2testing.resultat.external.model.TestDetails
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
 * @param source 
 * @param testDetails 
 * @param allIssues 
 */
data class AxeAuditor(

    @field:Valid
    @get:JsonProperty("source") val source: Source? = null,

    @field:Valid
    @get:JsonProperty("testDetails") val testDetails: TestDetails? = null,

    @field:Valid
    @get:JsonProperty("allIssues") val allIssues: kotlin.collections.List<AllIssue>? = null
) {

}

