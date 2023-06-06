package no.uutilsynet.testlab2testing.dto

data class Testregel(
    val id: Int,
    val krav: String,
    val testregelNoekkel: String,
    val kravTilSamsvar: String,
) {
  companion object {
    fun validateTestRegel(krav: String, testregelNoekkel: String, kravTilSamsvar: String) =
        if (krav.isBlank()) {
          throw IllegalArgumentException("Krav kan ikkje vera blank")
        } else if (testregelNoekkel.isBlank()) {
          throw IllegalArgumentException("Testregelnøkkel kan ikkje vera blank")
        } else if (kravTilSamsvar.isBlank()) {
          throw IllegalArgumentException("Krav til samsvar kan ikkje vera blank")
        } else if (!testregelNoekkel.matches(
            "^(QW-ACT-R)[0-9]{1,2}$".toRegex(RegexOption.IGNORE_CASE))) {
          throw IllegalArgumentException("TestregelNoekkel må vera på formen QW-ACT-RXX")
        } else {
          true
        }

    fun Testregel.validateTestRegel() = validateTestRegel(krav, testregelNoekkel, kravTilSamsvar)
  }
}
