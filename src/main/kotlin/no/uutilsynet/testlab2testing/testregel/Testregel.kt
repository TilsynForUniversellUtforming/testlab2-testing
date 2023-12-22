package no.uutilsynet.testlab2testing.testregel

import java.time.LocalDate
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.common.validateNamn

data class Testregel(
    val id: Int,
    val testregelId: String,
    val versjon: Int,
    val namn: String,
    val krav: String,
    val status: TestregelStatus,
    val datoSistEndra: LocalDate,
    val innholdstype: TestregelInnholdstype,
    val type: TestregelType,
    val spraak: TestlabLocale,
    val tema: Int,
    val testobjekt: Int,
    val kravTilSamsvar: String,
    val testregelSchema: String,
) {
  companion object {
    fun Testregel.validateTestregel(): Result<Testregel> = runCatching {
      val name = validateNamn(this.namn).getOrThrow()
      val krav = validateKrav(this.krav).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.type).getOrThrow()
      val sistEndra = LocalDate.now()

      Testregel(
          this.id,
          this.testregelId,
          this.versjon,
          name,
          krav,
          this.status,
          sistEndra,
          this.innholdstype,
          type,
          this.spraak,
          this.tema,
          this.testobjekt,
          this.kravTilSamsvar,
          schema)
    }
  }
}
