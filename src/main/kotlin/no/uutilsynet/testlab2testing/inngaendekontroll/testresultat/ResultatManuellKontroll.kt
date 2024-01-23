package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar

data class ResultatManuellKontroll(
    val id: Int,
    val sakId: Int,
    val loeysingId: Int,
    val testregelId: Int,
    val nettsideId: Int,
    val brukar: Brukar,
    val elementOmtale: String?,
    val elementResultat: String?,
    val elementUtfall: String?,
    val svar: List<Svar>,
    val testVartUtfoert: Instant?,
    val status: Status? = null
) {
  data class Svar(val steg: String, val svar: String)

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes(
      JsonSubTypes.Type(value = Status.IkkePaabegynt::class, name = "ikke_paabegynt"),
      JsonSubTypes.Type(value = Status.Paabegynt::class, name = "paabegynt"),
      JsonSubTypes.Type(value = Status.Ferdig::class, name = "ferdig"),
      JsonSubTypes.Type(value = Status.Deaktivert::class, name = "deaktivert"))
  sealed class Status {
    object IkkePaabegynt : Status()

    object Paabegynt : Status()

    data class Ferdig(
        val elementOmtale: String,
        val elementResultat: String,
        val elementUtfall: String,
        val testVartUtfoert: Instant
    ) : Status()

    object Deaktivert : Status()
  }
}
