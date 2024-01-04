package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.validateNamn

data class TestregelResponse(
    val id: Int,
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType,
) {
  companion object {
    fun TestregelResponse.validateTestregel(): Result<TestregelResponse> = runCatching {
      val name = validateNamn(this.name).getOrThrow()
      val krav = validateKrav(this.krav).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.type).getOrThrow()

      TestregelResponse(this.id, name, krav, schema, type)
    }
  }

  constructor(
      testregel: Testregel
  ) : this(testregel.id, testregel.namn, testregel.testregelSchema, testregel.krav, testregel.modus)
}
