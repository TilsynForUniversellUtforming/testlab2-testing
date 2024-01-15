package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar

data class ResultatManuellKontroll(
    val id: Int,
    val sakId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val nettsideId: Int,
    val brukar: Brukar,
    val elementOmtale: String?,
    val elementResultat: String?,
    val elementUtfall: String?,
    val svar: List<Svar>,
    val testVartUtfoert: Instant?
) {
  data class Svar(val steg: String, val svar: String)
}
