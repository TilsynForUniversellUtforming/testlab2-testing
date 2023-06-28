package no.uutilsynet.testlab2testing.loeysing

import java.net.URL

data class Loeysing(val id: Int, val namn: String, val url: URL, val orgnummer: String) {
  data class External(val namn: String, val url: String, val orgnummer: String)
}
