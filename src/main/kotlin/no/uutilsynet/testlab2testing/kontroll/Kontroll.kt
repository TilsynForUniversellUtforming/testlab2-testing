package no.uutilsynet.testlab2testing.kontroll

import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
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
    val sideutvalList: List<SideutvalElement> = emptyList(),
    val opprettaDato: LocalDate = LocalDate.now(),
) {

  data class Testreglar(val regelsettId: Int? = null, val testregelList: List<TestregelBase>)
}
