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
 * @param id 
 * @param issueSource 
 * @param issueId 
 * @param ruleId 
 * @param issueRule 
 * @param description 
 * @param help 
 * @param helpUrl 
 * @param impact 
 * @param summary 
 * @param type 
 * @param method 
 * @param status 
 * @param tags 
 * @param createdAt 
 * @param unitName 
 * @param testUrl 
 * @param unitType 
 * @param groupname 
 * @param foundBy 
 * @param lastUpdatedBy 
 * @param checkpoint 
 * @param source 
 * @param needsReview 
 * @param isExperimental 
 * @param isManual 
 * @param screenshotUrl 
 * @param issueUrl 
 * @param rule 
 */
data class AllIssue(

    @get:JsonProperty("id") val id: kotlin.String? = null,

    @get:JsonProperty("issue_source") val issueSource: kotlin.String? = null,

    @get:JsonProperty("issueId") val issueId: kotlin.Int? = null,

    @get:JsonProperty("ruleId") val ruleId: kotlin.String? = null,

    @get:JsonProperty("issue_rule") val issueRule: kotlin.String? = null,

    @get:JsonProperty("description") val description: kotlin.String? = null,

    @get:JsonProperty("help") val help: kotlin.String? = null,

    @get:JsonProperty("helpUrl") val helpUrl: kotlin.String? = null,

    @get:JsonProperty("impact") val impact: kotlin.String? = null,

    @get:JsonProperty("summary") val summary: kotlin.String? = null,

    @get:JsonProperty("type") val type: kotlin.String? = null,

    @get:JsonProperty("method") val method: kotlin.String? = null,

    @get:JsonProperty("status") val status: kotlin.String? = null,

    @get:JsonProperty("tags") val tags: kotlin.collections.List<kotlin.String>? = null,

    @get:JsonProperty("createdAt") val createdAt: kotlin.String? = null,

    @get:JsonProperty("unitName") val unitName: kotlin.String? = null,

    @get:JsonProperty("testUrl") val testUrl: kotlin.String? = null,

    @get:JsonProperty("unitType") val unitType: kotlin.String? = null,

    @field:Valid
    @get:JsonProperty("groupname") val groupname: kotlin.Any? = null,

    @get:JsonProperty("foundBy") val foundBy: kotlin.String? = null,

    @get:JsonProperty("lastUpdatedBy") val lastUpdatedBy: kotlin.String? = null,

    @get:JsonProperty("checkpoint") val checkpoint: kotlin.String? = null,

    @get:JsonProperty("source") val source: kotlin.collections.List<kotlin.String>? = null,

    @field:Valid
    @get:JsonProperty("needsReview") val needsReview: kotlin.Any? = null,

    @get:JsonProperty("isExperimental") val isExperimental: kotlin.Boolean? = null,

    @get:JsonProperty("isManual") val isManual: kotlin.Boolean? = null,

    @get:JsonProperty("screenshotUrl") val screenshotUrl: kotlin.collections.List<kotlin.String>? = null,

    @get:JsonProperty("issueUrl") val issueUrl: kotlin.String? = null,

    @get:JsonProperty("rule") val rule: kotlin.String? = null
) {

}

