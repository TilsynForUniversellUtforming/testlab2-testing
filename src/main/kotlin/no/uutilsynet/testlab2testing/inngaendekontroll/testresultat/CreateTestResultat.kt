package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

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
  constructor(
      testgrunnlagId: Int,
      loeysingId: Int,
      testregelId: Int,
      sideutvalId: Int,
      brukar: Brukar? = null,
      elementOmtale: String? = null,
      elementResultat: TestresultatUtfall? = null,
      elementUtfall: String? = null,
      elementUtfallId: Int? = null,
      elementOmtaleHtml: String? = null,
      elementPlassering: String? = null,
      testVartUtfoert: Instant? = null,
      kommentar: String? = null,
      imageDataUrl: String? = null,
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
      testVartUtfoert,
      kommentar,
      imageDataUrl,
      ekstraSjekk,
  )

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
