package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
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
      val result = validateLoeysingIdList(a, validIds)
      assertTrue(result.isSuccess)
    }

    @Test
    @DisplayName("når input inneholder ugyldige id-er, så skal valideringen feile")
    fun notOk() {
      val a = listOf(1, 2, 3, 11)
      val result = validateLoeysingIdList(a, validIds)
      assertTrue(result.isFailure)
    }

    @Test
    @DisplayName("når input er tom, så skal det feile")
    fun tomInput() {
      val results = listOf(null, emptyList<Int>()).map { validateLoeysingIdList(it, validIds) }
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
            listOf(URL("https://www.uutilsynet.no")),
            URL("https://www.status.url"),
            TestConstants.uutilsynetLoeysing,
            Instant.now())

    @DisplayName(
        "når vi prøver å gå til TestingFerdig, og det finnes testkjøringer som ikke er ferdig, så skal det ikke gå")
    @Test
    fun toTestingFails() {
      val maaling =
          Maaling.Testing(
              1,
              "navn",
              listOf(
                  TestKoeyring.Starta(
                      crawlResultatForUUTilsynet, Instant.now(), URL("https://www.status.url"))))
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
              listOf(
                  TestKoeyring.Ferdig(
                      crawlResultatForUUTilsynet,
                      Instant.now(),
                      URL("https://status.url"),
                      TestKoeyringTest.testResultater())))
      val result = Maaling.toTestingFerdig(maaling)
      Assertions.assertThat(result).isNotNull
    }
  }
}
