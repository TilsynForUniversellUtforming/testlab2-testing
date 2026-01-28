package no.uutilsynet.testlab2testing.testregel.model

import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateTestregelId
import no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x
import no.uutilsynet.testlab2testing.testregel.validateSchema

data class Testregel(
    override val id: Int,
    val testregelId: String,
    override val namn: String,
    override val kravId: Int,
    val status: TestregelStatus,
    override val type: TestregelInnholdstype,
    override val modus: TestregelModus,
    val tema: Int?,
    val testobjekt: Int?,
    val testregelSchema: String,
    val innhaldstypeTesting: Int?,
) : TestregelBase(id, namn, kravId, modus, type) {
  companion object {
    fun Testregel.validateTestregel(): Result<Testregel> = runCatching {
      val name = validateNamn(this.namn).getOrThrow()
      val testregelId = validateTestregelId(this.testregelId).getOrThrow()
      val schema = validateSchema(this.testregelSchema, this.modus).getOrThrow()

      Testregel(
          this.id,
          testregelId,
          name,
          kravId,
          this.status,
          this.type,
          modus,
          this.tema,
          this.testobjekt,
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

data class TestregelKrav(
    val id: Int,
    val testregelId: String,
    val namn: String,
    val krav: KravWcag2x,
) {

  constructor(
      testregel: Testregel,
      krav: KravWcag2x
  ) : this(testregel.id, testregel.testregelId, testregel.namn, krav)
}
