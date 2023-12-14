package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.digdirLoeysing
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue

class MaalingKtTest {
  @Nested
  @DisplayName("validering av status")
  inner class ValidateStatus {
    @Test
    @DisplayName("'crawling' er en gyldig status")
    fun crawlingIsOk() {
      assertThat(validateStatus("crawling"), equalTo(Result.success(Status.Crawling)))
    }

    @Test
    @DisplayName("'testing' er en gyldig status")
    fun testingIsOk() {
      assertThat(validateStatus("testing"), equalTo(Result.success(Status.Testing)))
    }

    @Test
    @DisplayName("en ugyldig status skal feile")
    fun invalidStatus() {
      val s = "lvgifulgkn"
      assertThat(validateStatus(s).isFailure, equalTo(true))
    }
  }

  @Nested
  @DisplayName("validering av idliste")
  inner class ValidateIdList {
    private val validIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

    @Test
    @DisplayName("når input er en liste med gyldige id-er, så skal valideringen gi ok")
    fun ok() {
      val a = listOf(1, 2, 3)
      val result = validateIdList(a, validIds, "loeysingIdList")
      assertTrue(result.isSuccess)
    }

    @Test
    @DisplayName("når input inneholder ugyldige id-er, så skal valideringen feile")
    fun notOk() {
      val a = listOf(1, 2, 3, 11)
      val result = validateIdList(a, validIds, "loeysingIdList")
      assertTrue(result.isFailure)
    }

    @Test
    @DisplayName("når input er tom, så skal det feile")
    fun tomInput() {
      val results =
          listOf(null, emptyList<Int>()).map { validateIdList(it, validIds, "loeysingIdList") }
      assertTrue(results[0].isFailure)
      assertTrue(results[1].isFailure)
    }
  }

  @Nested
  @DisplayName("Validering av crawlparameter")
  inner class ValidateCrawlParameters {
    @Test
    @DisplayName("Minste utvalg er 1 side")
    fun min10() {
      assertThrows<IllegalArgumentException> { CrawlParameters(0, 1).validateParameters() }
      assertThrows<IllegalArgumentException> { CrawlParameters(1, 0).validateParameters() }
      assertDoesNotThrow { CrawlParameters(1, 1).validateParameters() }
    }

    @Test
    @DisplayName("Største utvalg er 10000 for brutt og 2000 for netto")
    fun max10000() {
      assertThrows<IllegalArgumentException> { CrawlParameters(10001, 2000).validateParameters() }
      assertThrows<IllegalArgumentException> { CrawlParameters(10000, 2001).validateParameters() }
      assertDoesNotThrow { CrawlParameters(10000, 2000).validateParameters() }
    }
  }

  @DisplayName("bytte tilstand fra `testing` til `testing_ferdig`")
  @Nested
  inner class TestingFerdigTests {
    private val crawlResultatForUUTilsynet =
        CrawlResultat.Ferdig(
            1, URI("https://www.status.url").toURL(), uutilsynetLoeysing, Instant.now())
    private val crawlResultatForDigdir =
        CrawlResultat.Ferdig(
            1, URI("https://www.status.url").toURL(), digdirLoeysing, Instant.now())

    @DisplayName(
        "man skal ikke gå til status kvalitetssikring hvis noen crawlresultat har status starta")
    @Test
    fun toKvalitetssikringWithCrawlResultStarta() {
      val maaling =
          Maaling.Crawling(
              id = 1,
              crawlResultat =
                  listOf(
                      CrawlResultat.Ferdig(
                          1, URI("https://www.status.url").toURL(), digdirLoeysing, Instant.now()),
                      CrawlResultat.Starta(
                          URI("https://www.status.url").toURL(),
                          uutilsynetLoeysing,
                          Instant.now(),
                          Framgang(10, 100))),
              navn = "Test",
              datoStart = Instant.now())

      val maalingCrawling = Maaling.toKvalitetssikring(maaling)

      assertThat(maalingCrawling).isNull()
    }

    @DisplayName(
        "man skal ikke gå til status kvalitetssikring hvis noen crawlresultat har status ikkje starta")
    @Test
    fun toKvalitetssikringWithCrawlResultIkkjeStarta() {
      val maaling =
          Maaling.Crawling(
              id = 1,
              crawlResultat =
                  listOf(
                      CrawlResultat.Ferdig(
                          1, URI("https://www.status.url").toURL(), digdirLoeysing, Instant.now()),
                      CrawlResultat.IkkjeStarta(
                          URI("https://www.status.url").toURL(),
                          uutilsynetLoeysing,
                          Instant.now())),
              navn = "Test",
              datoStart = Instant.now())

      val maalingCrawling = Maaling.toKvalitetssikring(maaling)

      assertThat(maalingCrawling).isNull()
    }

    @DisplayName(
        "man skal gå til status kvalitetssikring hvis alle crawlresultat har status ferdig")
    @Test
    fun toKvalitetssikring() {
      val maaling =
          Maaling.Crawling(
              id = 1,
              crawlResultat =
                  listOf(
                      CrawlResultat.Ferdig(
                          1, URI("https://www.status.url").toURL(), digdirLoeysing, Instant.now()),
                      CrawlResultat.Ferdig(
                          1,
                          URI("https://www.status.url").toURL(),
                          uutilsynetLoeysing,
                          Instant.now())),
              navn = "Test",
              datoStart = Instant.now())

      val maalingKvalitetssikring = Maaling.toKvalitetssikring(maaling)

      assertThat(maalingKvalitetssikring).isInstanceOf(Maaling.Kvalitetssikring::class.java)
    }

    @DisplayName(
        "når vi prøver å gå til TestingFerdig, og det finnes testkjøringer som ikke er ferdig, så skal det ikke gå")
    @Test
    fun toTestingFails() {
      val maaling =
          Maaling.Testing(
              1,
              "navn",
              maalingDateStart,
              listOf(
                  TestKoeyring.Starta(
                      crawlResultatForUUTilsynet,
                      Instant.now(),
                      URI("https://www.status.url").toURL(),
                      Framgang(0, 0))))
      val result = Maaling.toTestingFerdig(maaling)
      assertThat(result).isNull()
    }

    @DisplayName(
        "når vi prøver å gå til `testing_ferdig`, og alle testkjøringer er ferdige, så skal det gå bra")
    @Test
    fun toTestingSucceeds() {
      val maaling =
          Maaling.Testing(
              1,
              "navn",
              maalingDateStart,
              listOf(
                  TestKoeyring.Ferdig(
                      crawlResultatForUUTilsynet,
                      Instant.now(),
                      URI("https://status.url").toURL(),
                      lenker)))
      val result = Maaling.toTestingFerdig(maaling)
      assertThat(result).isNotNull
    }

    @DisplayName(
        "når vi prøver å gå til `testing_ferdig` med en testkjøring som er ferdig, og en som har feila, så skal det gå bra")
    @Test
    fun toTestingFerdigFeila() {
      val maaling =
          Maaling.Testing(
              1,
              "navn",
              maalingDateStart,
              listOf(
                  TestKoeyring.Feila(crawlResultatForDigdir, Instant.now(), "autotester krasja"),
                  TestKoeyring.Ferdig(
                      crawlResultatForUUTilsynet,
                      Instant.now(),
                      URI("https://status.url").toURL(),
                      lenker),
              ))
      val result = Maaling.toTestingFerdig(maaling)
      assertThat(result).isNotNull
    }
  }

  @DisplayName("findFerdigeTestKoeyringar")
  @Nested
  inner class FindFerdigeTestKoeyringar {
    private val maaling =
        Maaling.TestingFerdig(
            1000,
            "test",
            maalingDateStart,
            listOf(
                TestKoeyring.Ferdig(
                    CrawlResultat.Ferdig(
                        1,
                        URI("https://www.status.url").toURL(),
                        uutilsynetLoeysing,
                        Instant.now()),
                    Instant.now(),
                    URI("https://www.status.url").toURL(),
                    lenker),
                TestKoeyring.Ferdig(
                    CrawlResultat.Ferdig(
                        1, URI("https://www.status.url").toURL(), digdirLoeysing, Instant.now()),
                    Instant.now(),
                    URI("https://www.status.url").toURL(),
                    lenker)))

    @DisplayName(
        "når vi henter testkøyringar for ei måling, uten å spesifisere løysing, så skal vi få alle testkøyringane")
    @Test
    fun alleTestKoeyringar() {
      val testKoeyringar = Maaling.findFerdigeTestKoeyringar(maaling)
      assertThat(testKoeyringar, hasSize(2))
    }

    @DisplayName(
        "når vi henter testkøyringar for ei måling, og spesifiserer ei løysing, så skal vi få testkøyringane for den løysinga")
    @Test
    fun testKoeyringarForLoeysing() {
      val testKoeyringar = Maaling.findFerdigeTestKoeyringar(maaling, uutilsynetLoeysing.id)
      assertThat(testKoeyringar, hasSize(1))
      assertThat(testKoeyringar[0].crawlResultat.loeysing, equalTo(uutilsynetLoeysing))
    }
  }
}

val lenker =
    AutoTesterClient.AutoTesterOutput.Lenker(
        URI("https://fullt.resultat").toURL(),
        URI("https://brot.resultat").toURL(),
        URI("https://aggregeringTR.resultat").toURL(),
        URI("https://aggregeringSK.resultat").toURL(),
        URI("https://aggregeringSide.resultat").toURL(),
        URI("https://aggregeringSideTR.resultat").toURL(),
        URI("https://aggregeringLoeysing.resultat").toURL(),
    )
