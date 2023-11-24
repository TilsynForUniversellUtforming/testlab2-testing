package no.uutilsynet.testlab2testing.dto

import no.uutilsynet.testlab2testing.common.validateJSONString
import no.uutilsynet.testlab2testing.testregel.TestregelType

data class Testregel(
    val id: Int,
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType,
) {

  companion object {
    fun validateTestRegel(
        name: String,
        testregelSchema: String,
        type: TestregelType,
        krav: String
    ) =
        if (krav.isBlank()) {
          throw IllegalArgumentException("Krav kan ikkje vera blank")
        } else if (testregelSchema.isBlank()) {
          throw IllegalArgumentException("Testregelnøkkel kan ikkje vera blank")
        } else if (name.isBlank()) {
          throw IllegalArgumentException("Krav til samsvar kan ikkje vera blank")
        } else if (type == TestregelType.forenklet &&
            !testregelSchema.matches("^(QW-ACT-R)[0-9]{1,2}$".toRegex(RegexOption.IGNORE_CASE))) {
          throw IllegalArgumentException("QualWeb regel id må vera på formen QW-ACT-RXX")
        } else if (type == TestregelType.inngaaende &&
            validateJSONString(testregelSchema).isFailure) {
          throw IllegalArgumentException("Skjema må væra på gylig json-format")
        } else {
          true
        }

    fun Testregel.validateTestRegel() = validateTestRegel(name, testregelSchema, type, krav)
  }
}
