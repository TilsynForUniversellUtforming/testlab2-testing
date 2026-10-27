package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar

interface ResultatManuellKontrollBase {
  val testgrunnlagId: Int
  val loeysingId: Int
  val testregelId: Int
  val sideutvalId: Int
  val brukar: Brukar
  val element: Element?
  val svar: List<Svar>
  val testVartUtfoert: Instant?
  val status: ResultatStatus
  val kommentar: String?
  val sistLagra: Instant
  val ekstraSjekk: Boolean

  val elementResultat: TestresultatUtfall?
    get() = element?.elementResultat

  val elementUtfall: String?
    get() = element?.elementUtfall

  val elementOmtaleHtml: String?
    get() = element?.elementOmtaleHtml
}

data class CreateRetestResultatManuellKontroll(
    override val testgrunnlagId: Int,
    override val loeysingId: Int,
    override val testregelId: Int,
    override val sideutvalId: Int,
    override val brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
    override val element: Element? = null,
    override val svar: List<Svar>,
    override val testVartUtfoert: Instant?,
    override val status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
    override val kommentar: String?,
    override val sistLagra: Instant,
    override val ekstraSjekk: Boolean = false,
) : ResultatManuellKontrollBase {
  constructor(
      testgrunnlagId: Int,
      loeysingId: Int,
      testregelId: Int,
      sideutvalId: Int,
      brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
      elementOmtale: String?,
      elementResultat: TestresultatUtfall?,
      elementUtfall: String?,
      elementUtfallId: Int? = null,
      elementOmtaleHtml: String? = null,
      elementPlassering: String? = null,
      svar: List<Svar>,
      testVartUtfoert: Instant?,
      status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
      kommentar: String?,
      sistLagra: Instant,
      ekstraSjekk: Boolean = false,
  ) : this(
      testgrunnlagId,
      loeysingId,
      testregelId,
      sideutvalId,
      brukar,
      Element(
          elementOmtale = elementOmtale,
          elementResultat = elementResultat,
          elementUtfall = elementUtfall,
          elementUtfallId = elementUtfallId,
          elementOmtaleHtml = elementOmtaleHtml,
          elementPlassering = elementPlassering,
      ),
      svar,
      testVartUtfoert,
      status,
      kommentar,
      sistLagra,
      ekstraSjekk,
  )
}

data class Svar(
    val steg: String,
    val svar: String,
)

enum class ResultatStatus {
  IkkjePaabegynt,
  UnderArbeid,
  Ferdig,
  Deaktivert
}

data class Element(
    val elementOmtale: String?,
    val elementResultat: TestresultatUtfall?,
    val elementUtfall: String?,
    val elementUtfallId: Int? = null,
    val elementOmtaleHtml: String? = null,
    val elementPlassering: String? = null,
)

data class ResultatManuellKontroll(
    val id: Int,
    override val testgrunnlagId: Int,
    override val loeysingId: Int,
    override val testregelId: Int,
    override val sideutvalId: Int,
    override val brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
    override val element: Element? = null,
    override val svar: List<Svar>,
    override val testVartUtfoert: Instant?,
    override val status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
    override val kommentar: String?,
    override val sistLagra: Instant,
    override val ekstraSjekk: Boolean = false,
) : ResultatManuellKontrollBase {
  constructor(
      id: Int,
      testgrunnlagId: Int,
      loeysingId: Int,
      testregelId: Int,
      sideutvalId: Int,
      brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
      elementOmtale: String?,
      elementResultat: TestresultatUtfall?,
      elementUtfall: String?,
      elementUtfallId: Int? = null,
      svar: List<Svar>,
      testVartUtfoert: Instant?,
      status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
      kommentar: String?,
      sistLagra: Instant,
      elementOmtaleHtml: String? = null,
      ekstraSjekk: Boolean = false,
      elementPlassering: String? = null,
  ) : this(
      id,
      testgrunnlagId,
      loeysingId,
      testregelId,
      sideutvalId,
      brukar,
      Element(
          elementOmtale = elementOmtale,
          elementResultat = elementResultat,
          elementUtfall = elementUtfall,
          elementUtfallId = elementUtfallId,
          elementOmtaleHtml = elementOmtaleHtml,
          elementPlassering = elementPlassering,
      ),
      svar,
      testVartUtfoert,
      status,
      kommentar,
      sistLagra,
      ekstraSjekk,
  )
}
