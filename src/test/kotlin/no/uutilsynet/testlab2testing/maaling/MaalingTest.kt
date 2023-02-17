package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.maaling.TestConstants.loeysingList
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MaalingTest {
  @Test
  @DisplayName("vi skal kunne oppdatere status fra planlegging til crawling")
  fun updateStatus() {
    val result =
        Maaling.updateStatus(Maaling.Planlegging(1, "testmåling", loeysingList), "crawling")
            .getOrThrow()

    Assertions.assertTrue(result is Maaling.Crawling)
    assertThat(result.id, equalTo(1))
    assertThat(result.navn, equalTo("testmåling"))
  }

  @Nested
  @DisplayName("validering av status")
  inner class ValidateStatus {
    @Test
    @DisplayName("'crawling' er en gyldig ny status")
    fun crawlingIsOk() {
      assertThat(validateStatus("crawling"), equalTo(Result.success("crawling")))
    }
  }
}
