package no.uutilsynet.testlab2testing.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ValidatorsKtTest {
  @ParameterizedTest
  @ValueSource(strings = ["123456785", "938644500"])
  @DisplayName("når vi validerer eit gyldig orgnummer, så skal vi få success")
  fun gyldigOrgNummer(s: String) {
    val result = validateOrgNummer(s)
    assertThat(result).isEqualTo(Result.success(s))
  }

  @Test
  @DisplayName("når vi validerer eit ugyldig orgnummer, så skal vi få failure")
  fun ugyldigOrgNummer() {
    val orgnummer = "123456789"
    val result = validateOrgNummer(orgnummer)
    assertThat(result.isFailure).isTrue()
  }

  @Test
  @DisplayName("når vi validerer noko anna enn eit orgnummer, så skal vi få failure")
  fun ikkjeEitOrgNummer() {
    val orgnummer = "hello world"
    val result = validateOrgNummer(orgnummer)
    assertThat(result.isFailure).isTrue()
  }

  @Test
  @DisplayName("når vi validerer null, så skal vi få failure")
  fun nullOrgNummer() {
    val orgnummer = null
    val result = validateOrgNummer(orgnummer)
    assertThat(result.isFailure).isTrue()
  }
}
