package no.uutilsynet.testlab2testing.kontroll

import com.fasterxml.jackson.annotation.JsonProperty
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelBase

data class Kontroll(
    val id: Int,
    val kontrolltype: KontrollType,
    val tittel: String,
    val saksbehandler: String,
    val sakstype: Sakstype,
    val arkivreferanse: String,
    val utval: Utval? = null,
    val testreglar: KontrollTestregler? = null,
) {
  enum class Sakstype {
    @JsonProperty("forvaltningssak") Forvaltningssak,
    @JsonProperty("arkivsak") Arkivsak
  }

  enum class KontrollType {
    @JsonProperty("manuell-kontroll") ManuellKontroll
  }

  data class KontrollTestregler(
      val regelsettId: Int? = null,
      val testregelList: List<TestregelBase>
  )
}
