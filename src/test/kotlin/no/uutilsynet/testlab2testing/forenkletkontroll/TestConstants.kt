package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.Verksemd
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestKoeyring
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.Testregel
import java.net.URI
import java.time.Instant

object TestConstants {

    val uutilsynetVerksemd = Verksemd(
        id = 1,
        namn = "UUTilsynet",
        organisasjonsnummer = "991825827",
    )

    val digdirVerksemd = Verksemd(
        id = 2,
        namn = "Digdir",
        organisasjonsnummer = "991825827",
    )

    val uutilsynetLoeysingExpanded =
      Loeysing.Expanded(
          1, "UUTilsynet", URI("https://www.uutilsynet.no/").toURL(), uutilsynetVerksemd)

    val uutilsynetLoeysing = uutilsynetLoeysingExpanded.toLoeysing()


    //create test Verksemd for UUTilsynet
  val digdirLoeysingExpanded =
        Loeysing.Expanded(2, "Digdir", URI("https://www.digdir.no/").toURL(), digdirVerksemd)

    val digdirLoeysing = digdirLoeysingExpanded.toLoeysing()
  val loeysingList = listOf(uutilsynetLoeysing, digdirLoeysing)

  val maalingTestName = "test_skal_slettes"
  val testregel =
      Testregel(
          1,
          "QW-ACT-12",
          1,
          TestConstants.name,
          TestConstants.testregelTestKravId,
          TestregelStatus.publisert,
          Instant.now(),
          TestregelInnholdstype.nett,
          TestregelModus.automatisk,
          TestlabLocale.nb,
          1,
          1,
          "QW-ACT-12",
          TestConstants.testregelSchemaAutomatisk,
          1)

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
          crawlResultat.loeysing,
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
          ),
          Brukar("test", "testar"),
          crawlResultat.antallNettsider)

  val testKoeyring2 =
      TestKoeyring.Ferdig(
          crawlResultat2.loeysing,
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
          ),
          Brukar("test", "testar"),
          crawlResultat2.antallNettsider)

  val testKoeyringList = listOf(testKoeyring, testKoeyring2)
}

fun Loeysing.Expanded.toLoeysing(): Loeysing {
    return Loeysing(id, namn, url, verksemd!!.organisasjonsnummer, verksemd!!.namn)
}
