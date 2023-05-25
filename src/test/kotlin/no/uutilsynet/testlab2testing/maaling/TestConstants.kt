package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing

object TestConstants {
  val uutilsynetLoeysing = Loeysing(1, "UUTilsynet", URL("https://www.uutilsynet.no/"))
  val digdirLoeysing = Loeysing(2, "Digdir", URL("https://www.digdir.no/"))
  val loeysingList = listOf(uutilsynetLoeysing, digdirLoeysing)
  val maalingTestName = "test_skal_slettes"

  val maalingRequestBody =
      mapOf(
          "navn" to maalingTestName,
          "loeysingIdList" to loeysingList.map { it.id },
          "crawlParameters" to mapOf("maxLinksPerPage" to 10, "numLinksToSelect" to 10))

  const val statusURL = "https://status.url"
  val crawlResultat =
      CrawlResultat.Ferdig(
          listOf(
              URL("https://www.uutilsynet.no/"),
              URL("https://www.uutilsynet.no/underside/1"),
              URL("https://www.uutilsynet.no/underside/2")),
          URL("https://status.url"),
          uutilsynetLoeysing,
          Instant.now())
}
