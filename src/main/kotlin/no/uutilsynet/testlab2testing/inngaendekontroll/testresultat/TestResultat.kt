package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant

data class TestResultat(
    val id: Int,
    val sakId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val nettsideId: Int,
    val elementOmtale: String,
    val elementResultat: String,
    val elementUtfall: String,
    val testVartUtfoert: Instant,
    val brukarId: Int
)
