package no.uutilsynet.testlab2testing.resultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall

data class TestresultatDB(
    val id: Int,
    val testgrunnlagId: Int?,
    val maalingId: Int?,
    val testregelId: Int,
    val loeysingId: Int,
    val sideutvalId: Int,
    val testUtfoert: Instant,
    val elementUtfall: String,
    val elementResultat: TestresultatUtfall,
    val elementOmtalePointer: String,
    val elmentOmtaleHtml: String,
    val elementOmtaleDescription: String,
    val brukarId: Int,
)

data class TestresultatDBBase(
    val testgrunnlagId: Int?,
    val maalingId: Int?,
    val testregelId: Int,
    val loeysingId: Int,
    val sideutvalId: Int,
    val testUtfoert: Instant,
    val elementUtfall: String,
    val elementResultat: TestresultatUtfall,
    val elementOmtalePointer: String,
    val elmentOmtaleHtml: String,
    val elementOmtaleDescription: String,
    val brukarId: Int,
)
