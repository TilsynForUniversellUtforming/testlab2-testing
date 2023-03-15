package no.uutilsynet.testlab2testing.maaling

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertTrue
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
}
