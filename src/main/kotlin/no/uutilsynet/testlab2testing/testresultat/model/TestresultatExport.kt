package no.uutilsynet.testlab2testing.testresultat.model

import jakarta.persistence.*
import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall

data class TestresultatExport(
    val testrunUuid: String,
    val loeysingId: Int,
    val testregelId: Int,
    val sideutvalId: Int,
    val testUtfoert: Instant,
    val elementUtfall: String,
    val elementResultat: TestresultatUtfall,
    val elementOmtalePointer: String,
    val elementOmtaleHtml: String,
    val elementOmtaleDescription: String,
    val brukarId: Int
)
