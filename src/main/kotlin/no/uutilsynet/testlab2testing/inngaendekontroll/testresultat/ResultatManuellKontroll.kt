package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar

open class ResultatManuellKontrollBase(
    open val testgrunnlagId: Int,
    open val loeysingId: Int,
    open val testregelId: Int,
    open val sideutvalId: Int,
    open val brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
    open val elementOmtale: String?,
    open val elementResultat: TestresultatUtfall?,
    open val elementUtfall: String?,
    open val svar: List<Svar>,
    open val testVartUtfoert: Instant?,
    open val status: Status = Status.IkkjePaabegynt,
    open val kommentar: String?,
    open val sistLagra: Instant
) {
  data class Svar(val steg: String, val svar: String)

  enum class Status {
    IkkjePaabegynt,
    UnderArbeid,
    Ferdig,
    Deaktivert
  }
}

data class ResultatManuellKontroll(
    val id: Int,
    override val testgrunnlagId: Int,
    override val loeysingId: Int,
    override val testregelId: Int,
    override val sideutvalId: Int,
    override val brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
    override val elementOmtale: String?,
    override val elementResultat: TestresultatUtfall?,
    override val elementUtfall: String?,
    override val svar: List<Svar>,
    override val testVartUtfoert: Instant?,
    override val status: Status = Status.IkkjePaabegynt,
    override val kommentar: String?,
    override val sistLagra: Instant
) :
    ResultatManuellKontrollBase(
        testgrunnlagId,
        loeysingId,
        testregelId,
        sideutvalId,
        brukar,
        elementOmtale,
        elementResultat,
        elementUtfall,
        svar,
        testVartUtfoert,
        status,
        kommentar,
        sistLagra)
