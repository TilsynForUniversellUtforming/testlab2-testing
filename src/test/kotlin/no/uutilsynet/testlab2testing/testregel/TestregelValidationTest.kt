package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaForenklet
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaInngaaende
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TestregelValidationTest {
  @Test
  @DisplayName("Gyldig testregel for forenklet kontroll skal kunne valideres")
  fun validForenklet() {
    assertDoesNotThrow {
      Testregel(1, name, testregelSchemaForenklet, testregelTestKrav, TestregelType.forenklet)
          .validateTestregel()
    }
  }

  @Test
  @DisplayName("Gyldig testregel for inngående kontroll skal kunne valideres")
  fun validInngaaende() {
    assertDoesNotThrow {
      Testregel(1, name, testregelSchemaInngaaende, testregelTestKrav, TestregelType.inngaaende)
          .validateTestregel()
    }
  }

  @Nested
  @DisplayName("For validering av testregel-objektet skal ")
  inner class invalidTestregel {

    @Test
    @DisplayName("ugyldig namn feile")
    fun invalidName() {
      val testregel =
          Testregel(1, "", testregelSchemaForenklet, testregelTestKrav, TestregelType.forenklet)
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig krav feile")
    fun invalidKrav() {
      val testregel = Testregel(1, name, testregelSchemaForenklet, "", TestregelType.forenklet)
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig testregelSchema for forenklet kontroll feile")
    fun invalidSchemaForenket() {
      val testregel = Testregel(1, name, "", testregelTestKrav, TestregelType.forenklet)
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig testregelSchema for forenklet inngaaende feile")
    fun invalidSchemaInngaaende() {
      val testregel = Testregel(1, name, "", testregelTestKrav, TestregelType.inngaaende)
      assertTrue(testregel.validateTestregel().isFailure)
    }
  }

  @Test
  @DisplayName(
      "TestregelSchema for forenklet kontroll må være på act-regel format og være riktig formattert")
  fun testregelSchemaActError() {
    val schema = validateSchema("qw-act-r12", TestregelType.forenklet)
    assertTrue(schema.isFailure)
  }

  @Test
  @DisplayName(
      "TestregelSchema for forenklet kontroll med riktig act-regel format skal være gyldig")
  fun testregelSchemaActSuccess() {
    val schema = validateSchema(testregelSchemaForenklet, TestregelType.forenklet)
    assertTrue(schema.isSuccess)
  }

  @Test
  @DisplayName("TestregelSchema for inngående kontroll må ha gyldig json-format")
  fun testregelSchemaWcagJsonError() {
    val schema = validateSchema("{1}", TestregelType.inngaaende)
    assertTrue(schema.isFailure)
  }

  @Test
  @DisplayName("TestregelSchema for inngående kontroll med riktig json-format skal være gyldig")
  fun testregelSchemaWcagJsonSuccess() {
    val schema = validateSchema(testregelSchemaInngaaende, TestregelType.inngaaende)
    assertTrue(schema.isSuccess)
  }

  companion object {
    @JvmStatic fun invalidParamsSource(): List<String?> = listOf("", null)
  }

  @Nested
  @DisplayName("For påkrevde felter må")
  inner class ValidateRequiredFields {
    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidationTest#invalidParamsSource")
    @DisplayName("namn være definert")
    fun nameError(invalidParam: String?) {
      val name = validateName(invalidParam)
      assertTrue(name.isFailure)
    }

    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidationTest#invalidParamsSource")
    @DisplayName("krav være definert")
    fun kravError(invalidParam: String?) {
      val krav = validateKrav(invalidParam)
      assertTrue(krav.isFailure)
    }

    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidationTest#invalidParamsSource")
    @DisplayName("TestregelSchema være definert for forenklet kontroll")
    fun testregelSchemaErrorForenklet(invalidParam: String?) {
      val schema = validateSchema(invalidParam, TestregelType.forenklet)
      assertTrue(schema.isFailure)
    }

    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidationTest#invalidParamsSource")
    @DisplayName("TestregelSchema være definert for inngående kontroll")
    fun testregelSchemaErrorInngaaende(invalidParam: String?) {
      val schema = validateSchema(invalidParam, TestregelType.inngaaende)
      assertTrue(schema.isFailure)
    }
  }
}
