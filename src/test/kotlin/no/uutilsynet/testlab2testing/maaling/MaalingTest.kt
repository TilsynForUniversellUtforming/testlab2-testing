package no.uutilsynet.testlab2testing.maaling

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MaalingTest {
  @Nested
  @DisplayName("validering av status")
  inner class ValidateStatus {
    @Test
    @DisplayName("'crawling' er en gyldig ny status")
    fun crawlingIsOk() {
      assertThat(validateStatus("crawling"), equalTo(Result.success(Status.Crawling)))
    }
  }
}
