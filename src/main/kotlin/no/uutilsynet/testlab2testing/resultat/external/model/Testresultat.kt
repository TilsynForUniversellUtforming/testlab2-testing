package no.uutilsynet.testlab2testing.resultat.external.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import no.uutilsynet.testlab2testing.resultat.external.model.Testgrunnlag
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
 * @param testgrunnlag 
 * @param loeysingId 
 * @param testregelId 
 * @param sideutvalId 
 * @param testUtfoert 
 * @param elementUtfall 
 * @param elementResultat 
 * @param elementOmtalePointer 
 * @param elementOmtaleHtml 
 * @param elementOmtaleDescription 
 * @param brukarId 
 * @param testrunUuid 
 */
data class Testresultat(

    @get:JsonProperty("id") val id: kotlin.Long? = null,

    @field:Valid
    @get:JsonProperty("testgrunnlag") val testgrunnlag: Testgrunnlag? = null,

    @get:JsonProperty("loeysingId") val loeysingId: kotlin.Int? = null,

    @get:JsonProperty("testregelId") val testregelId: kotlin.Int? = null,

    @get:JsonProperty("sideutvalId") val sideutvalId: kotlin.Int? = null,

    @get:JsonProperty("testUtfoert") val testUtfoert: java.time.OffsetDateTime? = null,

    @get:JsonProperty("elementUtfall") val elementUtfall: kotlin.String? = null,

    @get:JsonProperty("elementResultat") val elementResultat: Testresultat.ElementResultat? = null,

    @get:JsonProperty("elementOmtalePointer") val elementOmtalePointer: kotlin.String? = null,

    @get:JsonProperty("elementOmtaleHtml") val elementOmtaleHtml: kotlin.String? = null,

    @get:JsonProperty("elementOmtaleDescription") val elementOmtaleDescription: kotlin.String? = null,

    @get:JsonProperty("brukarId") val brukarId: kotlin.Int? = null,

    @get:JsonProperty("testrunUuid") val testrunUuid: kotlin.String? = null
) {

    /**
    * 
    * Values: samsvar,brot,ikkjeForekomst,varsel,ikkjeTesta
    */
    enum class ElementResultat(@get:JsonValue val value: kotlin.String) {

        samsvar("samsvar"),
        brot("brot"),
        ikkjeForekomst("ikkjeForekomst"),
        varsel("varsel"),
        ikkjeTesta("ikkjeTesta");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): ElementResultat {
                return values().firstOrNull{it -> it.value == value}
                    ?: throw IllegalArgumentException("Unexpected value '$value' for enum 'Testresultat'")
            }
        }
    }

}

