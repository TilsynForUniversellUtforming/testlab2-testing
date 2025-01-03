package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar

data class CreateTestResultat(
    val testgrunnlagId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val sideutvalId: Int,
    val brukar: Brukar?,
    val elementOmtale: String? = null,
    val elementHtml: String? = null,
    val elementPointer: String? = null,
    val elementResultat: TestresultatUtfall? = null,
    val elementUtfall: String? = null,
    val testVartUtfoert: Instant? = null,
    val kommentar: String? = null,
)
