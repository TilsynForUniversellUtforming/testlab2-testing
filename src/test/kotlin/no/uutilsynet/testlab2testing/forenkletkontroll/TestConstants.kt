package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.*
import no.uutilsynet.testlab2testing.testregel.TestConstants

object TestConstants {
  val uutilsynetLoeysing =
      Loeysing(1, "UUTilsynet", URI("https://www.uutilsynet.no/").toURL(), "991825827")
  val digdirLoeysing = Loeysing(2, "Digdir", URI("https://www.digdir.no/").toURL(), "991825827")
  val loeysingList = listOf(uutilsynetLoeysing, digdirLoeysing)
  val loeysingListId = loeysingList.map { it.id }

  val maalingTestName = "test_skal_slettes"
  val testregel =
      Testregel(
          1,
          "QW-ACT-12",
          1,
          TestConstants.name,
          TestConstants.testregelTestKrav,
          TestregelStatus.publisert,
          LocalDate.now(),
          TestregelInnholdstype.nett,
          TestregelType.forenklet,
          TestlabLocale.nb,
          1,
          1,
          "QW-ACT-12",
          TestConstants.testregelSchemaForenklet)

  val testRegelList = listOf(testregel)

  val maalingDateStart = Instant.parse("2023-01-01T00:00:00Z")

  val maalingRequestBody =
      mapOf(
          "navn" to maalingTestName,
          "datoStart" to maalingDateStart,
          "loeysingIdList" to loeysingList.map { it.id },
          "testregelIdList" to testRegelList.map { it.id },
          "crawlParameters" to mapOf("maxLenker" to 10, "talLenker" to 10))

  const val statusURL = "https://status.url"
  val crawlResultat =
      CrawlResultat.Ferdig(3, URI("https://status.url").toURL(), uutilsynetLoeysing, Instant.now())

  val crawlResultat2 =
      CrawlResultat.Ferdig(3, URI("https://status.url").toURL(), digdirLoeysing, Instant.now())

  val testKoeyring =
      TestKoeyring.Ferdig(
          crawlResultat,
          Instant.now(),
          URI("https://status.url").toURL(),
          AutoTesterClient.AutoTesterLenker(
              URI("https://fullt.resultat").toURL(),
              URI("https://brot.resultat").toURL(),
              URI("https://aggregering.resultat").toURL(),
              URI("https://aggregeringSK.resultat").toURL(),
              URI("https://aggregeringSide.resultat").toURL(),
              URI("https://aggregeringSideTR.resultat").toURL(),
              URI("https://aggregeringLoeysing.resultat").toURL(),
          ))

  val testKoeyring2 =
      TestKoeyring.Ferdig(
          crawlResultat2,
          Instant.now(),
          URI("https://status.url").toURL(),
          AutoTesterClient.AutoTesterLenker(
              URI("https://fullt.resultat").toURL(),
              URI("https://brot.resultat").toURL(),
              URI("https://aggregering.resultat").toURL(),
              URI("https://aggregeringSK.resultat").toURL(),
              URI("https://aggregeringSide.resultat").toURL(),
              URI("https://aggregeringSideTR.resultat").toURL(),
              URI("https://aggregeringLoeysing.resultat").toURL(),
          ))

  val testKoeyringList = listOf(testKoeyring, testKoeyring2)
}
