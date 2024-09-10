package no.uutilsynet.testlab2testing.kontroll

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelBase

data class Kontroll(
    val id: Int,
    val kontrolltype: Kontrolltype,
    val tittel: String,
    val saksbehandler: String,
    val sakstype: Sakstype,
    val arkivreferanse: String,
    val utval: Utval? = null,
    val testreglar: Testreglar? = null,
    val sideutvalList: List<Sideutval> = emptyList(),
    val opprettaDato: LocalDate = LocalDate.now(),
) {
  enum class Sakstype {
    @JsonProperty("forvaltningssak") Forvaltningssak,
    @JsonProperty("arkivsak") Arkivsak
  }

  enum class Kontrolltype {
    @JsonProperty("inngaaende-kontroll") InngaaendeKontroll,
    @JsonProperty("forenkla-kontroll") ForenklaKontroll,
    @JsonProperty("tilsyn") Tilsyn,
    @JsonProperty("statusmaaling") Statusmaaling,
    @JsonProperty("uttalesak") Uttalesak,
  }

  data class Testreglar(val regelsettId: Int? = null, val testregelList: List<TestregelBase>)
}
