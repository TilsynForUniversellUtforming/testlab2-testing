package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Loeysingstype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType

data class TestingStatus(
    val loeysingId: Int,
    val loeysingNamn: String,
    val loeysingstype: Loeysingstype,
    val kontrollType: Kontrolltype,
    val testgrunnlagType: TestgrunnlagType,
    val styringsdataId: Int?,
    val styringdataStatus: StyringsdataStatus,
    val status: ManuellTestStatus,
    val kanSlette: Boolean,
    val kanReteste: Boolean,
    val teststatistics: TestStatusCount
)

enum class ManuellTestStatus {
  FERDIG,
  DEAKTIVERT,
  UNDER_ARBEID,
  IKKJE_STARTA
}

data class TestStatusCount(
    val loeysingId: Int,
    val testgrunnlagId: Int,
    val total: Int,
    val ferdig: Int,
    val underArbeid: Int,
    val ikkjeStarta: Int,
    val percentagePerSide: Double,
    val percentagePerInnholdstype: Double
)

enum class StyringsdataStatus {
  BOT,
  PAALEG,
  KLAGE,
  INGEN_REAKSJON_BRUKT
}
