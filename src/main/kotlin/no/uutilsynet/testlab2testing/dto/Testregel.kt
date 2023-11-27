package no.uutilsynet.testlab2testing.dto

import no.uutilsynet.testlab2testing.testregel.TestregelType
import no.uutilsynet.testlab2testing.testregel.validateKrav
import no.uutilsynet.testlab2testing.testregel.validateName
import no.uutilsynet.testlab2testing.testregel.validateSchema

data class Testregel(
    val id: Int,
    val name: String,
    val testregelSchema: String,
    val krav: String,
    val type: TestregelType,
) {
  companion object {
    fun Testregel.validateTestregel(): Result<Testregel> = runCatching {
      val name = validateName(this.name).getOrThrow()
      val krav = validateKrav(this.krav).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.type).getOrThrow()

      Testregel(this.id, name, krav, schema, type)
    }
  }
}
