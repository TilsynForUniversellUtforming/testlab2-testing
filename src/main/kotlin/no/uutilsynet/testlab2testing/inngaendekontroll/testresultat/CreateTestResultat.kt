package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar

data class CreateTestResultat(
    val testgrunnlagId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val sideutvalId: Int,
    val brukar: Brukar? = null,
    val element: Element? = null,
    val testVartUtfoert: Instant? = null,
    val kommentar: String? = null,
    val imageDataUrl: String? = null,
    val ekstraSjekk: Boolean = false,
) {
  companion object {
    @JsonCreator
    @JvmStatic
    fun create(
        @JsonProperty("testgrunnlagId") testgrunnlagId: Int,
        @JsonProperty("loeysingId") loeysingId: Int,
        @JsonProperty("testregelId") testregelId: Int,
        @JsonProperty("sideutvalId") sideutvalId: Int,
        @JsonProperty("brukar") brukar: Brukar? = null,
        @JsonProperty("element") element: Element? = null,
        @JsonProperty("elementOmtale") elementOmtale: String? = null,
        @JsonProperty("elementResultat") elementResultat: TestresultatUtfall? = null,
        @JsonProperty("elementUtfall") elementUtfall: String? = null,
        @JsonProperty("elementUtfallId") elementUtfallId: Int? = null,
        @JsonProperty("elementOmtaleHtml") elementOmtaleHtml: String? = null,
        @JsonProperty("elementPlassering") elementPlassering: String? = null,
        @JsonProperty("testVartUtfoert") testVartUtfoert: Instant? = null,
        @JsonProperty("kommentar") kommentar: String? = null,
        @JsonProperty("imageDataUrl") imageDataUrl: String? = null,
        @JsonProperty("ekstraSjekk") ekstraSjekk: Boolean = false,
    ): CreateTestResultat =
        CreateTestResultat(
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
            testVartUtfoert = testVartUtfoert,
            kommentar = kommentar,
            imageDataUrl = imageDataUrl,
            ekstraSjekk = ekstraSjekk,
        )
  }

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

  val elementPlassering: String?
    get() = element?.elementPlassering
}
