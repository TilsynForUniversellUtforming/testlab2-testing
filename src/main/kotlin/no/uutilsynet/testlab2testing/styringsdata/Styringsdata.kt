package no.uutilsynet.testlab2testing.styringsdata

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class StyringsdataListElement(
    val id: Int,
    val kontrollId: Int,
    val loeysingId: Int,
    val ansvarleg: String,
    val oppretta: Instant,
    val frist: Instant,
    val reaksjon: Reaksjonstype,
    val paaleggId: Int?,
    val paaleggKlageId: Int?,
    val botId: Int?,
    val botKlageId: Int?
)

data class Styringsdata(
    val id: Int?,
    val loeysingId: Int,
    val kontrollId: Int,
    val ansvarleg: String,
    val oppretta: Instant,
    val frist: Instant,
    val reaksjon: Reaksjonstype,
    val paalegg: Paalegg?,
    val paaleggKlage: Klage?,
    val bot: Bot?,
    val botKlage: Klage?,
)

data class Paalegg(
    val id: Int?,
    val vedtakDato: Instant,
    val frist: Instant?,
)

data class Klage(
    val id: Int?,
    val klageType: Klagetype,
    val klageMottattDato: Instant,
    val klageAvgjortDato: Instant?,
    val resultatKlageTilsyn: ResultatKlage?,
    val klageDatoDepartement: Instant?,
    val resultatKlageDepartement: ResultatKlage?
)

data class Bot(
    val id: Int?,
    val beloepDag: Int,
    val oekingEtterDager: Int,
    val oekningType: BotOekningType,
    val oekingSats: Int,
    val vedtakDato: Instant,
    val startDato: Instant,
    val sluttDato: Instant?,
    val kommentar: String?,
)

enum class Klagetype {
  paalegg,
  bot
}

enum class Reaksjonstype {
  @JsonProperty("reaksjon") reaksjon,
  @JsonProperty("ingen-reaksjon") ingenReaksjon
}

enum class BotOekningType {
  @JsonProperty("kroner") kroner,
  @JsonProperty("prosent") prosent,
  @JsonProperty("ikkje-relevant") ikkjeRelevant
}

enum class ResultatKlage {
  @JsonProperty("stadfesta") stadfesta,
  @JsonProperty("delvis-omgjort") delvisOmgjort,
  @JsonProperty("omgjort") omgjort,
  @JsonProperty("oppheva") oppheva,
}
