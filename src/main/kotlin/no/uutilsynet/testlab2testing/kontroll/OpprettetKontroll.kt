package no.uutilsynet.testlab2testing.kontroll

import com.fasterxml.jackson.annotation.JsonProperty

data class OpprettetKontroll(
    val id: Int,
    val kontrolltype: KontrollType,
    val tittel: String,
    val saksbehandler: String,
    val sakstype: Sakstype,
    val arkivreferanse: String,
) {
  enum class Sakstype {
    @JsonProperty("forvaltningssak") Forvaltningssak,
    @JsonProperty("arkivsak") Arkivsak
  }

  enum class KontrollType {
    @JsonProperty("manuell-kontroll") ManuellKontroll
  }
}
