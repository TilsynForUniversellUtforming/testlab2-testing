package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.common.validateNamn

data class TestregelDTO(
    val id: Int,
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelModus,
    val innhaldstypeTesting: Int?,
) {
  fun toTestregel() {
    TODO("Not yet implemented")
  }

  companion object {
    fun TestregelDTO.validateTestregel(): Result<TestregelDTO> = runCatching {
      val name = validateNamn(this.name).getOrThrow()
      val krav = validateKrav(this.krav).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.type).getOrThrow()

      TestregelDTO(this.id, name, krav, schema, type, this.innhaldstypeTesting)
    }
  }

  constructor(
      testregel: Testregel
  ) : this(
      testregel.id,
      testregel.namn,
      testregel.testregelSchema,
      testregel.krav,
      testregel.modus,
      testregel.innhaldstypeTesting)
}
