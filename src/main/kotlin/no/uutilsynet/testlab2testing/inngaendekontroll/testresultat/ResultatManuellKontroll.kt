package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.dto.TestresultatUtfall

data class ResultatManuellKontroll(
    val id: Int,
    val sakId: Int,
    val loeysingId: Int,
    val testregelId: Int, // Databaseid for testregel
    val nettsideId: Int,
    val brukar: Brukar,
    val elementOmtale: String?,
    val elementResultat: TestresultatUtfall?,
    val elementUtfall: String?,
    val svar: List<Svar>,
    val testVartUtfoert: Instant?,
    val status: Status = Status.IkkjePaabegynt
) {
  data class Svar(val steg: String, val svar: String)

  enum class Status {
    IkkjePaabegynt,
    UnderArbeid,
    Ferdig,
    Deaktivert
  }
}
