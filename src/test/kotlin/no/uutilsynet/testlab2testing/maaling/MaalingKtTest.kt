package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.common.validateIdList
import no.uutilsynet.testlab2testing.common.validateStatus
import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingDateStart
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

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
    @DisplayName("Minste utvalg er 10 sider")
    fun min10() {
      assertThrows<IllegalArgumentException> { CrawlParameters(9, 10).validateParameters() }
      assertThrows<IllegalArgumentException> { CrawlParameters(10, 9).validateParameters() }
      assertDoesNotThrow { CrawlParameters(10, 10).validateParameters() }
    }

    @Test
    @DisplayName("Største utvalg er 2000 sider")
    fun max2000() {
      assertThrows<IllegalArgumentException> { CrawlParameters(2001, 2000).validateParameters() }
      assertThrows<IllegalArgumentException> { CrawlParameters(2000, 2001).validateParameters() }
      assertDoesNotThrow { CrawlParameters(2000, 2000).validateParameters() }
    }
  }

  @DisplayName("bytte tilstand fra `testing` til `testing_ferdig`")
  @Nested
  inner class TestingFerdigTests {
    private val crawlResultatForUUTilsynet =
        CrawlResultat.Ferdig(
            listOf(URI("https://www.uutilsynet.no").toURL()),
            URI("https://www.status.url").toURL(),
            TestConstants.uutilsynetLoeysing,
            Instant.now())
    private val crawlResultatForDigdir =
        CrawlResultat.Ferdig(
            listOf(URI("https://www.digdir.no").toURL()),
            URI("https://www.status.url").toURL(),
            TestConstants.digdirLoeysing,
            Instant.now())

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
      Assertions.assertThat(result).isNull()
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
                      TestKoeyringTest.testResultater())))
      val result = Maaling.toTestingFerdig(maaling)
      Assertions.assertThat(result).isNotNull
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
                      TestKoeyringTest.testResultater()),
              ))
      val result = Maaling.toTestingFerdig(maaling)
      Assertions.assertThat(result).isNotNull
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
                        listOf(URI("https://www.uutilsynet.no").toURL()),
                        URI("https://www.status.url").toURL(),
                        TestConstants.uutilsynetLoeysing,
                        Instant.now()),
                    Instant.now(),
                    URI("https://www.status.url").toURL(),
                    emptyList(),
                    AutoTesterClient.AutoTesterOutput.Lenker(
                        URI("https://fullt.resultat").toURL(),
                        URI("https://brot.resultat").toURL())),
                TestKoeyring.Ferdig(
                    CrawlResultat.Ferdig(
                        listOf(URI("https://www.digdir.no").toURL()),
                        URI("https://www.status.url").toURL(),
                        TestConstants.digdirLoeysing,
                        Instant.now()),
                    Instant.now(),
                    URI("https://www.status.url").toURL(),
                    emptyList(),
                    AutoTesterClient.AutoTesterOutput.Lenker(
                        URI("https://fullt.resultat").toURL(),
                        URI("https://brot.resultat").toURL()))))

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
      val testKoeyringar =
          Maaling.findFerdigeTestKoeyringar(maaling, TestConstants.uutilsynetLoeysing.id)
      assertThat(testKoeyringar, hasSize(1))
      assertThat(
          testKoeyringar[0].crawlResultat.loeysing, equalTo(TestConstants.uutilsynetLoeysing))
    }
  }
}
