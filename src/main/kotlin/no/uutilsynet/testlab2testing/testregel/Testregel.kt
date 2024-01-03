package no.uutilsynet.testlab2testing.testregel

import java.time.LocalDate
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateTestregelId

data class Testregel(
    val id: Int,
    val testregelId: String,
    val versjon: Int,
    val namn: String,
    val krav: String,
    val status: TestregelStatus,
    val datoSistEndra: LocalDate,
    val type: TestregelInnholdstype,
    val modus: TestregelType,
    val spraak: TestlabLocale,
    val tema: Int?,
    val testobjekt: Int?,
    val kravTilSamsvar: String?,
    val testregelSchema: String,
) {
  companion object {
    fun Testregel.validateTestregel(): Result<Testregel> = runCatching {
      val name = validateNamn(this.namn).getOrThrow()
      val krav = validateKrav(this.krav).getOrThrow()
      val testregelId = validateTestregelId(this.testregelId).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.modus).getOrThrow()
      val sistEndra = LocalDate.now()

      Testregel(
          this.id,
          testregelId,
          this.versjon,
          name,
          krav,
          this.status,
          sistEndra,
          this.type,
          modus,
          this.spraak,
          this.tema,
          this.testobjekt,
          this.kravTilSamsvar,
          schema)
    }
  }
}
