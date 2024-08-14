package no.uutilsynet.testlab2testing.styringsdata

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate

data class StyringsdataListElement(
    val id: Int,
    val kontrollId: Int,
    val loeysingId: Int,
    val ansvarleg: String,
    val oppretta: LocalDate,
    val frist: LocalDate,
    val reaksjon: Reaksjonstype,
    val paaleggReaksjon: Reaksjonstype,
    val paaleggKlageReaksjon: Reaksjonstype,
    val botReaksjon: Reaksjonstype,
    val botKlageReaksjon: Reaksjonstype,
    val paaleggId: Int?,
    val paaleggKlageId: Int?,
    val botId: Int?,
    val botKlageId: Int?,
    val sistLagra: Instant
)

data class Styringsdata(
    val id: Int?,
    val loeysingId: Int,
    val kontrollId: Int,
    val ansvarleg: String,
    val oppretta: LocalDate,
    val frist: LocalDate,
    val reaksjon: Reaksjonstype,
    val paaleggReaksjon: Reaksjonstype,
    val paaleggKlageReaksjon: Reaksjonstype,
    val botReaksjon: Reaksjonstype,
    val botKlageReaksjon: Reaksjonstype,
    val paalegg: Paalegg?,
    val paaleggKlage: Klage?,
    val bot: Bot?,
    val botKlage: Klage?,
    val sistLagra: Instant?
)

data class Paalegg(
    val id: Int?,
    val vedtakDato: LocalDate,
    val frist: LocalDate?,
)

data class Klage(
    val id: Int?,
    val klageMottattDato: LocalDate,
    val klageAvgjortDato: LocalDate?,
    val resultatKlageTilsyn: ResultatKlage?,
    val klageDatoDepartement: LocalDate?,
    val resultatKlageDepartement: ResultatKlage?
)

data class Bot(
    val id: Int?,
    val beloepDag: Int,
    val oekingEtterDager: Int,
    val oekningType: BotOekningType,
    val oekingSats: Int,
    val vedtakDato: LocalDate,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
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
