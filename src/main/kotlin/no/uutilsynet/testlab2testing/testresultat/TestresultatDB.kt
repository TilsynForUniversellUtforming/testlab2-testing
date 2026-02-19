package no.uutilsynet.testlab2testing.testresultat

import java.time.Instant
import java.util.UUID
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.testresultat.model.TestresultatExport

data class TestresultatDB(
    val id: Int,
    val testgrunnlagId: Int?,
    val maalingId: Int?,
    val testregelId: Int,
    val loeysingId: Int,
    val sideutvalId: Int,
    val side: String,
    val testUtfoert: Instant,
    val elementUtfall: String,
    val elementResultat: TestresultatUtfall,
    val elementOmtalePointer: String?,
    val elmentOmtaleHtml: String?,
    val elementOmtaleDescription: String?,
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
    val elementOmtalePointer: String?,
    val elmentOmtaleHtml: String?,
    val elementOmtaleDescription: String?,
    val brukarId: Int,
) {
  fun toTestresultatExport(testrunId: UUID): TestresultatExport {
    return TestresultatExport(
        testrunUuid = testrunId.toString(),
        loeysingId = this.loeysingId,
        testregelId = this.testregelId,
        sideutvalId = this.sideutvalId,
        testUtfoert = this.testUtfoert,
        elementUtfall = this.elementUtfall,
        elementResultat = this.elementResultat,
        elementOmtalePointer = this.elementOmtalePointer ?: "",
        elementOmtaleHtml = this.elmentOmtaleHtml ?: "",
        elementOmtaleDescription = this.elementOmtaleDescription ?: "",
        brukarId = this.brukarId)
  }
}

// fun TestresultatDetaljert.toTestresultatDBBase(
//    detaljert: TestresultatDetaljert,
//    maalingId: Int? = null,
//    sideutvalId: Int? = null
// ): TestresultatDBBase {
//    return TestresultatDBBase(
//        testgrunnlagId = detaljert.testgrunnlagId,
//        maalingId = maalingId,
//        testregelId = detaljert.testregelId,
//        loeysingId = detaljert.loeysingId,
//        sideutvalId = sideutvalId,
//        testUtfoert =
// detaljert.testVartUtfoert?.atZone(java.time.ZoneId.systemDefault())?.toInstant()
//            ?: java.time.Instant.EPOCH,
//        elementUtfall = detaljert.elementUtfall ?: "",
//        elementResultat = detaljert.elementResultat ?: TestresultatUtfall.IKKE_TESTET,
//        elementOmtalePointer = detaljert.elementOmtale?.pointer,
//        elmentOmtaleHtml = detaljert.elementOmtale?.htmlCode,
//        elementOmtaleDescription = detaljert.elementOmtale?.description,
//        brukarId = detaljert.brukarId?.id ?: -1
//    )
// }
