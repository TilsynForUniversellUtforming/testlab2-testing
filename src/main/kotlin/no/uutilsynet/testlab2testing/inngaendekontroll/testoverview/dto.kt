package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import com.fasterxml.jackson.annotation.JsonProperty
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
    val styringsdataStatus: StyringsdataStatus,
    val status: ManuellTestStatus,
    val kanSlette: Boolean,
    val kanReteste: Boolean,
    val teststatistics: TestStatusCount
)

enum class ManuellTestStatus {
    @JsonProperty("ferdig")
    FERDIG,

    @JsonProperty("deaktivert")
    DEAKTIVERT,

    @JsonProperty("under-arbeid")
    UNDER_ARBEID,

    @JsonProperty("ikkje-starta")
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
  PAALEGG,
  KLAGE,
  INGEN_REAKSJON_BRUKT
}
