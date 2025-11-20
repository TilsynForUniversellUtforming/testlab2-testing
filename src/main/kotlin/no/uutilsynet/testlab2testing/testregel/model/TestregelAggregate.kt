package no.uutilsynet.testlab2testing.testregel.model

import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.krav.KravWcag2x
import java.time.Instant

data class TestregelAggregate(
    val id: Int,
    val testregelId: String,
    val versjon: Int,
    val namn: String,
    val krav: KravWcag2x,
    val status: TestregelStatus,
    val datoSistEndra: Instant = Instant.now(),
    val type: TestregelInnholdstype,
    val modus: TestregelModus,
    val spraak: TestlabLocale,
    val tema: Tema?,
    val testobjekt: Testobjekt?,
    val kravTilSamsvar: String?,
    val testregelSchema: String,
    val innhaldstypeTesting: InnhaldstypeTesting?
)