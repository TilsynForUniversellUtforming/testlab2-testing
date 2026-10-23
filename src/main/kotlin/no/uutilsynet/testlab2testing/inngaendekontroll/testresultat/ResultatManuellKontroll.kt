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
    open val element: Element? = null,
    open val svar: List<Svar>,
    open val testVartUtfoert: Instant?,
    open val status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
    open val kommentar: String?,
    open val sistLagra: Instant,
    open val ekstraSjekk: Boolean = false,
) {
  val elementOmtale: String?
    get() = element?.elementOmtale

  val elementResultat: TestresultatUtfall?
    get() = element?.elementResultat

  val elementUtfall: String?
    get() = element?.elementUtfall

  val elementUtfallId: Int?
    get() = element?.elementUtfallId

  val elementOmtaleHtml: String?
    get() = element?.elementOmtaleHtml

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
) :
    ResultatManuellKontrollBase(
        testgrunnlagId,
        loeysingId,
        testregelId,
        sideutvalId,
        brukar,
        element,
        svar,
        testVartUtfoert,
        status,
        kommentar,
        sistLagra,
        ekstraSjekk,
    ) {
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
      ),
      svar,
      testVartUtfoert,
      status,
      kommentar,
      sistLagra,
      ekstraSjekk,
  )
}
