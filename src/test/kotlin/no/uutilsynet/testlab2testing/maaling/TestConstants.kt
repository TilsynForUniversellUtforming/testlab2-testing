package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testregel.TestConstants

object TestConstants {
  val uutilsynetLoeysing = Loeysing(1, "UUTilsynet", URL("https://www.uutilsynet.no/"))
  val digdirLoeysing = Loeysing(2, "Digdir", URL("https://www.digdir.no/"))
  val loeysingList = listOf(uutilsynetLoeysing, digdirLoeysing)
  val maalingTestName = "test_skal_slettes"
  val testregel =
      Testregel(
          1,
          TestConstants.testregelTestKrav,
          "QW-ACT-12",
          TestConstants.testregelTestKravTilSamsvar,
      )

  val testRegelList = listOf(testregel)

  val maalingRequestBody =
      mapOf(
          "navn" to maalingTestName,
          "loeysingIdList" to loeysingList.map { it.id },
          "testregelIdList" to testRegelList.map { it.id },
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

  val crawlResultat2 =
      CrawlResultat.Ferdig(
          listOf(
              URL("https://www.digdir.no/"),
              URL("https://www.digdir.no/underside/1"),
              URL("https://www.digdir.no/underside/2")),
          URL("https://status.url"),
          digdirLoeysing,
          Instant.now())

  val testKoeyring =
      TestKoeyring.Ferdig(
          crawlResultat,
          Instant.now(),
          URL("https://status.url"),
          emptyList(),
          AutoTesterClient.AutoTesterOutput.Lenker(
              URL("https://fullt.resultat"), URL("https://brot.resultat")))

  val testKoeyring2 =
      TestKoeyring.Ferdig(
          crawlResultat2,
          Instant.now(),
          URL("https://status.url"),
          emptyList(),
          AutoTesterClient.AutoTesterOutput.Lenker(
              URL("https://fullt.resultat"), URL("https://brot.resultat")))

  val testKoeyringList = listOf(testKoeyring, testKoeyring2)
}
