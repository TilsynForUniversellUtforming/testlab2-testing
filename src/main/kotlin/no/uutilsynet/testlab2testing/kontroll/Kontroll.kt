package no.uutilsynet.testlab2testing.kontroll

import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.Utval

data class Kontroll(
    val id: Int,
    val kontrolltype: KontrollType,
    val tittel: String,
    val saksbehandler: String,
    val sakstype: Sakstype,
    val arkivreferanse: String,
    val loeysingar: List<Loeysing>,
    val utval: Utval? = null
) {
  enum class Sakstype {
    @JsonProperty("forvaltningssak") Forvaltningssak,
    @JsonProperty("arkivsak") Arkivsak
  }

  enum class KontrollType {
    @JsonProperty("manuell-kontroll") ManuellKontroll
  }
}
