package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.validateJSONString

fun validateSchema(testregelSchema: String?, modus: TestregelModus): Result<String> = runCatching {
  if (testregelSchema.isNullOrBlank()) {
    throw IllegalArgumentException("Testregel-skjema kan ikkje vera blank")
  }

  if (modus == TestregelModus.forenklet &&
      !testregelSchema.matches("^(QW-ACT-R)[0-9]{1,2}$".toRegex())) {
    throw IllegalArgumentException("QualWeb regel id må vera på formen QW-ACT-RXX")
  } else if (modus == TestregelModus.manuell && validateJSONString(testregelSchema).isFailure) {
    throw IllegalArgumentException("Skjema må væra på gylig json-format")
  }

  testregelSchema
}
