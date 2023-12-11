package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant

sealed class ResultatManuellKontroll {
  data class UnderArbeid(
      val id: Int,
      val sakId: Int,
      val loeysingId: Int,
      val testregelId: Int,
      val nettsideId: Int,
      val elementOmtale: String?,
      val elementResultat: String?,
      val elementUtfall: String?,
      val svar: List<Svar>
  ) : ResultatManuellKontroll()

  data class Ferdig(
      val id: Int,
      val sakId: Int,
      val loeysingId: Int,
      val testregelId: Int,
      val nettsideId: Int,
      val elementOmtale: String,
      val elementResultat: String,
      val elementUtfall: String,
      val svar: List<Svar>,
      val testVartUtfoert: Instant
  ) : ResultatManuellKontroll()

  data class Svar(val steg: String, val svar: String)
}
