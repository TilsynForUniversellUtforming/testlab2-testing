package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchema
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
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
        Testregel(1, name, testregelSchema, testregelTestKrav, TestregelType.forenklet)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Krav må være definert")
    fun kravError() {
      assertThrows<IllegalArgumentException> {
        Testregel(1, "", testregelSchema, testregelTestKrav, TestregelType.forenklet)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("TestregelSchema må være definert")
    fun testregelSchemaError() {
      assertThrows<IllegalArgumentException> {
        Testregel(1, name, "", testregelTestKrav, TestregelType.forenklet).validateTestRegel()
      }
    }

    @Test
    @DisplayName(
        "TestregelSchema må kan kun være act-regel, og må ha riktig format for forenklet kontroll")
    fun testregelSchemaActError() {
      assertThrows<IllegalArgumentException> {
        Testregel(1, name, "noe-annet", testregelTestKrav, TestregelType.forenklet)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName(
        "TestregelSchema må kan kun være act-regel, og må ha riktig format for forenklet kontroll")
    fun testregelSchemaActSuccess() {
      assertDoesNotThrow {
        Testregel(1, name, "QW-ACT-R12", testregelTestKrav, TestregelType.forenklet)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("TestregelSchema må ha gyldig json-format for inngående kontroll")
    fun testregelSchemaWcagJsonError() {
      assertThrows<IllegalArgumentException> {
        Testregel(1, name, "{1}".trimIndent(), testregelTestKrav, TestregelType.inngaaende)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName(
        "TestregelSchema må kan kun være act-regel, og må ha riktig format for forenklet kontroll")
    fun testregelSchemaWcagJsonSuccess() {
      assertDoesNotThrow {
        Testregel(
                1,
                name,
                """
          {
            "gaaTil": 1
          }
          """
                    .trimIndent(),
                testregelTestKrav,
                TestregelType.inngaaende)
            .validateTestRegel()
      }
    }

    @Test
    @DisplayName("Krav til samsvar må være definert")
    fun nameError() {
      assertThrows<IllegalArgumentException> {
        Testregel(1, name, testregelSchema, "", TestregelType.forenklet).validateTestRegel()
      }
    }
  }
}
