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
 * @param testId 
 * @param engine 
 * @param bestPracticesEnabled 
 * @param experimentalEnabled 
 * @param product 
 * @param release 
 * @param platform 
 * @param environment 
 * @param digitalAssetType 
 * @param assistiveTechnology 
 * @param axeVersion 
 * @param testCaseName 
 * @param testRunName 
 * @param startDate 
 * @param endDate 
 * @param standard 
 */
data class TestDetails(

    @get:JsonProperty("testId") val testId: kotlin.String? = null,

    @get:JsonProperty("engine") val engine: kotlin.String? = null,

    @get:JsonProperty("bestPracticesEnabled") val bestPracticesEnabled: kotlin.Boolean? = null,

    @get:JsonProperty("experimentalEnabled") val experimentalEnabled: kotlin.Boolean? = null,

    @get:JsonProperty("product") val product: kotlin.String? = null,

    @get:JsonProperty("release") val release: kotlin.String? = null,

    @get:JsonProperty("platform") val platform: kotlin.String? = null,

    @get:JsonProperty("environment") val environment: kotlin.String? = null,

    @get:JsonProperty("digitalAssetType") val digitalAssetType: kotlin.String? = null,

    @get:JsonProperty("assistiveTechnology") val assistiveTechnology: kotlin.String? = null,

    @get:JsonProperty("axeVersion") val axeVersion: kotlin.String? = null,

    @get:JsonProperty("testCaseName") val testCaseName: kotlin.String? = null,

    @get:JsonProperty("testRunName") val testRunName: kotlin.String? = null,

    @get:JsonProperty("startDate") val startDate: kotlin.String? = null,

    @field:Valid
    @get:JsonProperty("endDate") val endDate: kotlin.Any? = null,

    @get:JsonProperty("standard") val standard: kotlin.String? = null
) {

}

