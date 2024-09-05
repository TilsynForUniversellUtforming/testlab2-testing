package no.uutilsynet.testlab2testing.styringsdata

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(Styringsdata.Kontroll::class, name = "kontroll"),
    JsonSubTypes.Type(Styringsdata.Loeysing::class, name = "loeysing"))
sealed class Styringsdata {
  abstract val id: Int?
  abstract val kontrollId: Int
  abstract val ansvarleg: String
  abstract val oppretta: LocalDate?
  abstract val frist: LocalDate?
  abstract val sistLagra: Instant?

  data class Loeysing(
      override val id: Int?,
      override val kontrollId: Int,
      override val ansvarleg: String,
      override val oppretta: LocalDate,
      override val frist: LocalDate,
      override val sistLagra: Instant?,
      val loeysingId: Int,
      val reaksjon: Reaksjonstype,
      val paaleggReaksjon: Reaksjonstype,
      val paaleggKlageReaksjon: Reaksjonstype,
      val botReaksjon: Reaksjonstype,
      val botKlageReaksjon: Reaksjonstype,
      val paalegg: Paalegg?,
      val paaleggKlage: Klage?,
      val bot: Bot?,
      val botKlage: Klage?,
  ) : Styringsdata() {
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
  }

  data class Kontroll(
      override val id: Int?,
      override val kontrollId: Int,
      override val ansvarleg: String,
      override val oppretta: LocalDate?,
      override val frist: LocalDate?,
      override val sistLagra: Instant?,
      val varselSendtDato: LocalDate?,
      val status: StyringsdataKontrollStatus?,
      val foerebelsRapportSendtDato: LocalDate?,
      val svarFoerebelsRapportDato: LocalDate?,
      val endeligRapportDato: LocalDate?,
      val kontrollAvsluttaDato: LocalDate?,
      val rapportPublisertDato: LocalDate?,
  ) : Styringsdata() {
    enum class StyringsdataKontrollStatus {
      @JsonProperty("planlagt") planlagt,
      @JsonProperty("paagar") paagar,
      @JsonProperty("avslutta") avslutta,
      @JsonProperty("ikkje-aktuell") ikkjeAktuell,
      @JsonProperty("forsinka") forsinka,
    }
  }
}

enum class StyringsdataType {
  kontroll,
  loeysing,
}

enum class Klagetype {
  paalegg,
  bot
}

enum class Reaksjonstype {
  @JsonProperty("reaksjon") reaksjon,
  @JsonProperty("ingen-reaksjon") ingenReaksjon
}

data class StyringsdataListElement(
    override val id: Int,
    override val kontrollId: Int,
    override val ansvarleg: String,
    override val oppretta: LocalDate,
    override val frist: LocalDate,
    override val sistLagra: Instant,
    val loeysingId: Int,
    val reaksjon: Reaksjonstype,
    val paaleggReaksjon: Reaksjonstype,
    val paaleggKlageReaksjon: Reaksjonstype,
    val botReaksjon: Reaksjonstype,
    val botKlageReaksjon: Reaksjonstype,
    val paaleggId: Int?,
    val paaleggKlageId: Int?,
    val botId: Int?,
    val botKlageId: Int?,
) : Styringsdata() {
  val isPaalegg: Boolean
    get() = paaleggReaksjon == Reaksjonstype.reaksjon

  val isBot: Boolean
    get() = botReaksjon == Reaksjonstype.reaksjon
}
