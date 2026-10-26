package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import java.time.Instant

data class CreateTestResultat(
    val testgrunnlagId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val sideutvalId: Int,
    val brukar: Brukar?,
    val elementOmtale: String? = null,
    val elementOmtaleHtml: String? = null,
    val elementResultat: TestresultatUtfall? = null,
    val elementUtfall: String? = null,
    val elementUtfallId: Int? = null,
    val elementPlassering: String? = null,
    val testVartUtfoert: Instant? = null,
    val kommentar: String? = null,
    val imageDataUrl: String? = null,
    val ekstraSjekk: Boolean = false,
)
