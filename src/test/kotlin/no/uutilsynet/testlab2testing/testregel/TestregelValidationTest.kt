package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravTilSamsvar
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestTestregelNoekkel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class TestregelValidationTest {
  @Nested
  @DisplayName("Validering av testregel")
  inner class ValidateTestregel {
    @Test
    @DisplayName("Gyldig testregel skal kunne valideres")
    fun valid() {
      assertDoesNotThrow {
        Testregel(
                1,
                testregelTestKrav,
                testregelTestTestregelNoekkel,
                testregelTestKravTilSamsvar,
            )
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Krav må være definert")
    fun kravError() {
      assertThrows<IllegalArgumentException> {
        Testregel(
                1,
                "",
                testregelTestTestregelNoekkel,
                testregelTestKravTilSamsvar,
            )
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Testregelnøkkel må være definert")
    fun testRegelNoekkelError() {
      assertThrows<IllegalArgumentException> {
        Testregel(
                1,
                testregelTestKrav,
                "",
                testregelTestKravTilSamsvar,
            )
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Testregelnøkkel må kan kun være act-regel, og må ha riktig format")
    fun testRegelNoekkelRegexError() {
      assertThrows<IllegalArgumentException> {
        Testregel(
                1,
                testregelTestKrav,
                "noe-annet",
                testregelTestKravTilSamsvar,
            )
            .validateTestRegel()
      }

      assertThrows<IllegalArgumentException> {
        Testregel(
                1,
                testregelTestKrav,
                "QW-ACT-123",
                testregelTestKravTilSamsvar,
            )
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Krav til samsvar må være definert")
    fun kravTilSamsvarError() {
      assertThrows<IllegalArgumentException> {
        Testregel(
                1,
                testregelTestKrav,
                testregelTestTestregelNoekkel,
                "",
            )
            .validateTestRegel()
      }
    }
  }
}
