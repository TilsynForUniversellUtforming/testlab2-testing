package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import no.uutilsynet.testlab2testing.dto.Loeysing

object TestConstants {
  val uutilsynetLoeysing = Loeysing(1, "UUTilsynet", URL("https://www.uutilsynet.no/"))
  val digdirLoeysing = Loeysing(2, "Digdir", URL("https://www.digdir.no/"))
  val loeysingList = listOf(uutilsynetLoeysing, digdirLoeysing)

  val maalingRequestBody = mapOf("navn" to "example", "loeysingList" to loeysingList)
}
