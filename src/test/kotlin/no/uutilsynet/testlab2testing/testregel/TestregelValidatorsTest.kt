package no.uutilsynet.testlab2testing.testregel

import java.time.Instant
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaForenklet
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaManuell
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.validateTestregel
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TestregelValidatorsTest {
  @Test
  @DisplayName("Gyldig testregel for forenklet kontroll skal kunne valideres")
  fun validForenklet() {
    assertDoesNotThrow {
      Testregel(
              1,
              name,
              1,
              name,
              testregelTestKrav,
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.forenklet,
              TestlabLocale.nb,
              1,
              1,
              testregelTestKrav,
              testregelSchemaForenklet)
          .validateTestregel()
    }
  }

  @Test
  @DisplayName("Gyldig testregel for manuell kontroll skal kunne valideres")
  fun validManuell() {
    assertDoesNotThrow {
      Testregel(
              1,
              name,
              1,
              name,
              testregelTestKrav,
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.inngaaende,
              TestlabLocale.nb,
              1,
              1,
              testregelTestKrav,
              testregelSchemaInngaaende)
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
          Testregel(
              1,
              "",
              1,
              "",
              testregelTestKrav,
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.forenklet,
              TestlabLocale.nb,
              1,
              1,
              testregelTestKrav,
              testregelSchemaForenklet)
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig krav feile")
    fun invalidKrav() {
      val testregel =
          Testregel(
              1,
              name,
              1,
              name,
              "",
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.forenklet,
              TestlabLocale.nb,
              1,
              1,
              "",
              testregelSchemaForenklet)
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig testregelSchema for forenklet kontroll feile")
    fun invalidSchemaForenket() {
      val testregel =
          Testregel(
              1,
              name,
              1,
              name,
              testregelTestKrav,
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.forenklet,
              TestlabLocale.nb,
              1,
              1,
              testregelTestKrav,
              "")
      assertTrue(testregel.validateTestregel().isFailure)
    }

    @Test
    @DisplayName("ugyldig testregelSchema for forenklet inngaaende feile")
    fun invalidSchemaInngaaende() {
      val testregel =
          Testregel(
              1,
              name,
              1,
              name,
              testregelTestKrav,
              TestregelStatus.publisert,
              Instant.now(),
              TestregelInnholdstype.nett,
              TestregelModus.inngaaende,
              TestlabLocale.nb,
              1,
              1,
              testregelTestKrav,
              "")
      assertTrue(testregel.validateTestregel().isFailure)
    }
  }

  @Test
  @DisplayName(
      "TestregelSchema for forenklet kontroll må være på act-regel format og være riktig formattert")
  fun testregelSchemaActError() {
    val schema = validateSchema("qw-act-r12", TestregelModus.forenklet)
    assertTrue(schema.isFailure)
  }

  @Test
  @DisplayName(
      "TestregelSchema for forenklet kontroll med riktig act-regel format skal være gyldig")
  fun testregelSchemaActSuccess() {
    val schema = validateSchema(testregelSchemaForenklet, TestregelModus.forenklet)
    assertTrue(schema.isSuccess)
  }

  @Test
  @DisplayName("TestregelSchema for manuell kontroll må ha gyldig json-format")
  fun testregelSchemaWcagJsonError() {
    val schema = validateSchema("{1}", TestregelType.manuell)
    assertTrue(schema.isFailure)
  }

  @Test
  @DisplayName("TestregelSchema for manuell kontroll med riktig json-format skal være gyldig")
  fun testregelSchemaWcagJsonSuccess() {
    val schema = validateSchema(testregelSchemaManuell, TestregelType.manuell)
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
        "no.uutilsynet.testlab2testing.testregel.TestregelValidatorsTest#invalidParamsSource")
    @DisplayName("krav være definert")
    fun kravError(invalidParam: String?) {
      val krav = validateKrav(invalidParam)
      assertTrue(krav.isFailure)
    }

    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidatorsTest#invalidParamsSource")
    @DisplayName("TestregelSchema være definert for forenklet kontroll")
    fun testregelSchemaErrorForenklet(invalidParam: String?) {
      val schema = validateSchema(invalidParam, TestregelModus.forenklet)
      assertTrue(schema.isFailure)
    }

    @ParameterizedTest
    @MethodSource(
        "no.uutilsynet.testlab2testing.testregel.TestregelValidatorsTest#invalidParamsSource")
    @DisplayName("TestregelSchema være definert for manuell kontroll")
    fun testregelSchemaErrorManuell(invalidParam: String?) {
      val schema = validateSchema(invalidParam, TestregelModus.manuell)
      assertTrue(schema.isFailure)
    }
  }
}
