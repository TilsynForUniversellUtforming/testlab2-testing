package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tilstand")
@JsonSubTypes(
    Type(TestKoeyring.IkkjeStarta::class, name = "ikkje_starta"),
    Type(TestKoeyring.Starta::class, name = "starta"),
    Type(TestKoeyring.Feila::class, name = "feila"))
sealed class TestKoeyring {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant

  data class IkkjeStarta(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring()

  data class Starta(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val statusURL: URL
  ) : TestKoeyring()

  data class Ferdig(override val loeysing: Loeysing, override val sistOppdatert: Instant) :
      TestKoeyring()

  data class Feila(
      override val loeysing: Loeysing,
      override val sistOppdatert: Instant,
      val feilmelding: String
  ) : TestKoeyring()

  companion object {
    fun from(loeysing: Loeysing, statusURL: URL): TestKoeyring =
        IkkjeStarta(loeysing, Instant.now(), statusURL)

    fun updateStatus(
        testKoeyring: TestKoeyring,
        response: AutoTesterClient.Response
    ): TestKoeyring =
        when (testKoeyring) {
          is IkkjeStarta ->
              when (response.runtimeStatus) {
                AutoTesterClient.RuntimeStatus.Running ->
                    Starta(testKoeyring.loeysing, Instant.now(), testKoeyring.statusURL)
                AutoTesterClient.RuntimeStatus.Completed ->
                    Ferdig(testKoeyring.loeysing, Instant.now())
                AutoTesterClient.RuntimeStatus.Failed ->
                    Feila(testKoeyring.loeysing, Instant.now(), response.output.toString())
                else -> testKoeyring
              }
          is Starta -> {
            when (response.runtimeStatus) {
              AutoTesterClient.RuntimeStatus.Pending ->
                  IkkjeStarta(testKoeyring.loeysing, Instant.now(), testKoeyring.statusURL)
              AutoTesterClient.RuntimeStatus.Completed ->
                  Ferdig(testKoeyring.loeysing, Instant.now())
              AutoTesterClient.RuntimeStatus.Failed ->
                  Feila(testKoeyring.loeysing, Instant.now(), response.output.toString())
              else -> testKoeyring
            }
          }
          else -> testKoeyring
        }
  }
}
