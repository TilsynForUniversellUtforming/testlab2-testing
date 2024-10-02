package no.uutilsynet.testlab2testing.testregel

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateTestregelId

data class Testregel(
    override val id: Int,
    val testregelId: String,
    val versjon: Int,
    override val namn: String,
    override val kravId: Int,
    val status: TestregelStatus,
    val datoSistEndra: Instant = Instant.now(),
    override val type: TestregelInnholdstype,
    override val modus: TestregelModus,
    val spraak: TestlabLocale,
    val tema: Int?,
    val testobjekt: Int?,
    val kravTilSamsvar: String?,
    val testregelSchema: String,
    val innhaldstypeTesting: Int?
) : TestregelBase(id, namn, kravId, modus, type) {
  companion object {
    fun Testregel.validateTestregel(): Result<Testregel> = runCatching {
      val name = validateNamn(this.namn).getOrThrow()
      val testregelId = validateTestregelId(this.testregelId).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.modus).getOrThrow()

      Testregel(
          this.id,
          testregelId,
          this.versjon,
          name,
          kravId,
          this.status,
          this.datoSistEndra,
          this.type,
          modus,
          this.spraak,
          this.tema,
          this.testobjekt,
          this.kravTilSamsvar,
          schema,
          this.innhaldstypeTesting)
    }

    fun Testregel.toTestregelBase(): TestregelBase =
        TestregelBase(
            id = this.id,
            namn = this.namn,
            kravId = this.kravId,
            modus = this.modus,
            type = this.type)
  }
}
