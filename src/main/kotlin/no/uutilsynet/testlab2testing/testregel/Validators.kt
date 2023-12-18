package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.validateJSONString

fun validateKrav(krav: String?): Result<String> = runCatching {
  if (krav.isNullOrBlank()) {
    throw IllegalArgumentException("Krav kan ikkje vera blank")
  }
  krav
}

fun validateSchema(testregelSchema: String?, type: TestregelType): Result<String> = runCatching {
  if (testregelSchema.isNullOrBlank()) {
    throw IllegalArgumentException("Testregel-skjema kan ikkje vera blank")
  }

  if (type == TestregelType.forenklet &&
      !testregelSchema.matches("^(QW-ACT-R)[0-9]{1,2}$".toRegex())) {
    throw IllegalArgumentException("QualWeb regel id må vera på formen QW-ACT-RXX")
  } else if (type == TestregelType.manuell && validateJSONString(testregelSchema).isFailure) {
    throw IllegalArgumentException("Skjema må væra på gylig json-format")
  }

  testregelSchema
}
