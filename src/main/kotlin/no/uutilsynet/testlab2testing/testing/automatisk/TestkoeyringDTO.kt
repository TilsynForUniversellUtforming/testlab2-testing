package no.uutilsynet.testlab2testing.testing.automatisk

import java.net.URL
import java.time.Instant

sealed class TestkoeyringDTO {
  abstract val maalingId: Int
  abstract val loeysingId: Int
  abstract val sistOppdatert: Instant
  abstract val brukarId: Int?
  abstract val lenkerTesta: Int?

  data class IkkjeStarta(
      override val maalingId: Int,
      override val loeysingId: Int,
      override val brukarId: Int?,
      override val lenkerTesta: Int?,
      override val sistOppdatert: Instant,
      val statusURL: URL?,
  ) : TestkoeyringDTO()

  data class Starta(
      override val maalingId: Int,
      override val loeysingId: Int,
      override val brukarId: Int?,
      override val lenkerTesta: Int?,
      override val sistOppdatert: Instant,
      val statusURL: URL?,
  ) : TestkoeyringDTO()

  data class Ferdig(
      override val maalingId: Int,
      override val loeysingId: Int,
      override val brukarId: Int?,
      override val lenkerTesta: Int?,
      override val sistOppdatert: Instant,
      val statusURL: URL?,
      val lenker: AutoTesterClient.AutoTesterLenker,
  ) : TestkoeyringDTO()

  data class Feila(
      override val maalingId: Int,
      override val loeysingId: Int,
      override val brukarId: Int?,
      override val lenkerTesta: Int?,
      override val sistOppdatert: Instant,
      val feilmelding: String,
  ) : TestkoeyringDTO()
}

enum class TestkoeyringStatus {
  ferdig,
  feila,
  starta,
  ikkje_starta
}
