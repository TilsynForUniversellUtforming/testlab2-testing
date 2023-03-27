package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.maaling.CrawlParameters.Companion.validateParameters
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class MaalingTest {
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
    @Test
    @DisplayName("når input er en liste med id-er, så skal valideringen gi ok")
    fun ok() {
      val a = listOf(1, 2, 3)
      val result = validateLoeysingIdList(a)
      assertTrue(result.isSuccess)
    }

    @Test
    @DisplayName("når input er tom, så skal det feile")
    fun tomInput() {
      val results = listOf(null, emptyList<Int>()).map { validateLoeysingIdList(it) }
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
}
