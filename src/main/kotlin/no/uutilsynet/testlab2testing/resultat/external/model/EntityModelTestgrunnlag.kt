package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
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
 * @param testrunUuid 
 * @param type 
 * @param datoOppretta 
 * @param kontrollId 
 * @param links 
 */
data class EntityModelTestgrunnlag(

    @get:JsonProperty("testrunUuid") val testrunUuid: kotlin.String? = null,

    @get:JsonProperty("type") val type: EntityModelTestgrunnlag.Type? = null,

    @get:JsonProperty("datoOppretta") val datoOppretta: java.time.OffsetDateTime? = null,

    @get:JsonProperty("kontrollId") val kontrollId: kotlin.Int? = null,

    @field:Valid
    @get:JsonProperty("_links") val links: kotlin.collections.Map<kotlin.String, Link>? = null
) {

    /**
    * 
    * Values: OPPRINNELEG_TEST,RETEST
    */
    enum class Type(@get:JsonValue val value: kotlin.String) {

        OPPRINNELEG_TEST("OPPRINNELEG_TEST"),
        RETEST("RETEST");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Type {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'EntityModelTestgrunnlag'")
            }
        }
    }

}

