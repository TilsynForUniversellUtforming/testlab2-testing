package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing

sealed class TestKoeyring {
  abstract val loeysing: Loeysing
  abstract val sistOppdatert: Instant

  data class IkkjeStarta(
      override val loeysing: Loeysing,
      val statusURL: URL,
      override val sistOppdatert: Instant
  ) : TestKoeyring()
  data class Feila(
      override val loeysing: Loeysing,
      val feilmelding: String,
      override val sistOppdatert: Instant
  ) : TestKoeyring()

  companion object {
    fun from(loeysing: Loeysing, statusURL: URL): TestKoeyring =
        IkkjeStarta(loeysing, statusURL, Instant.now())
  }
}
