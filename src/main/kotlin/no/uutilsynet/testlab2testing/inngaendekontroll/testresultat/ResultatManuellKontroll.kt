package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
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
    override val element: Element?,
    override val svar: List<Svar>,
    override val testVartUtfoert: Instant?,
    override val status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
    override val kommentar: String?,
    override val sistLagra: Instant,
    override val ekstraSjekk: Boolean = false,
) : ResultatManuellKontrollBase {
  companion object {
    @JsonCreator
    @JvmStatic
    fun create(
        @JsonProperty("id") id: Int,
        @JsonProperty("testgrunnlagId") testgrunnlagId: Int,
        @JsonProperty("loeysingId") loeysingId: Int,
        @JsonProperty("testregelId") testregelId: Int,
        @JsonProperty("sideutvalId") sideutvalId: Int,
        @JsonProperty("brukar") brukar: Brukar = Brukar("testesen@digdir.no", "Test Testesen"),
        @JsonProperty("element") element: Element? = null,
        @JsonProperty("elementOmtale") elementOmtale: String? = null,
        @JsonProperty("elementResultat") elementResultat: TestresultatUtfall? = null,
        @JsonProperty("elementUtfall") elementUtfall: String? = null,
        @JsonProperty("elementUtfallId") elementUtfallId: Int? = null,
        @JsonProperty("elementPlassering") elementPlassering: String? = null,
        @JsonProperty("ekstraSjekk") ekstraSjekk: Boolean = false,
        @JsonProperty("svar") svar: List<Svar>,
        @JsonProperty("testVartUtfoert") testVartUtfoert: Instant?,
        @JsonProperty("status") status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
        @JsonProperty("kommentar") kommentar: String?,
        @JsonProperty("sistLagra") sistLagra: Instant,
        @JsonProperty("elementOmtaleHtml") elementOmtaleHtml: String? = null,
    ): ResultatManuellKontroll =
        ResultatManuellKontroll(
            id = id,
            testgrunnlagId = testgrunnlagId,
            loeysingId = loeysingId,
            testregelId = testregelId,
            sideutvalId = sideutvalId,
            brukar = brukar,
            element = element
                    ?: Element(
                        elementOmtale = elementOmtale,
                        elementResultat = elementResultat,
                        elementUtfall = elementUtfall,
                        elementUtfallId = elementUtfallId,
                        elementOmtaleHtml = elementOmtaleHtml,
                        elementPlassering = elementPlassering,
                    ),
            svar = svar,
            testVartUtfoert = testVartUtfoert,
            status = status,
            kommentar = kommentar,
            sistLagra = sistLagra,
            ekstraSjekk = ekstraSjekk,
        )
  }

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
      elementPlassering: String? = null,
      ekstraSjekk: Boolean = false,
      svar: List<Svar>,
      testVartUtfoert: Instant?,
      status: ResultatStatus = ResultatStatus.IkkjePaabegynt,
      kommentar: String?,
      sistLagra: Instant,
      elementOmtaleHtml: String? = null,
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
