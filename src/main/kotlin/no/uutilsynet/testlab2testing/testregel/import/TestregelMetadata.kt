import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Relevante metadatat henta ut frå testregel kjeldekoden påå github
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class TestregelMetadata(
    @JsonProperty("namn")
    val namn: String,
    @JsonProperty("id")
    val id: String,
    @JsonProperty("versjon")
    val versjon: String,
    @JsonProperty("type")
    val type: String,
    @JsonProperty("kravTilSamsvar")
    val kravTilSamsvar:String,
    @JsonProperty("spraak")
    val spraak: String
)