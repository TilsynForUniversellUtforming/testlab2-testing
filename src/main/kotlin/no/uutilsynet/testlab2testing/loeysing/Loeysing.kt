package no.uutilsynet.testlab2testing.loeysing

import java.net.URL

data class Loeysing(
    val id: Int,
    val namn: String,
    val url: URL,
    val orgnummer: String,
    val verksemdNamn: String?
) {
  data class External(val namn: String, val url: String, val orgnummer: String)

  data class Expanded(val id: Int, val namn: String, val url: URL, val verksemd: Verksemd?)

  data class Simple(
      val id: Int,
      val namn: String,
      val url: URL,
      val orgnummer: String,
      val verksemdId: Int
  )
}
