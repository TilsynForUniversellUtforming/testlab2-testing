package no.uutilsynet.testlab2testing.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ValidatorsKtTest {
  @Test
  @DisplayName("når vi validerer eit gyldig orgnummer, så skal vi få success")
  fun gyldigOrgNummer() {
    val orgnummer = "123456785"
    val result = validateOrgNummer(orgnummer)
    assertThat(result).isEqualTo(Result.success(orgnummer))
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
