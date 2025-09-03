package no.uutilsynet.testlab2testing.krav

import no.uutilsynet.testlab2.constants.KravStatus
import no.uutilsynet.testlab2.constants.WcagPrinsipp
import no.uutilsynet.testlab2.constants.WcagRetninglinje
import no.uutilsynet.testlab2.constants.WcagSamsvarsnivaa

data class KravWcag2x(
    val id: Int,
    val tittel: String,
    val status: KravStatus,
    val innhald: String?,
    val gjeldAutomat: Boolean,
    val gjeldNettsider: Boolean,
    val gjeldApp: Boolean,
    val urlRettleiing: String?,
    val prinsipp: WcagPrinsipp,
    val retningslinje: WcagRetninglinje,
    val suksesskriterium: String,
    val samsvarsnivaa: WcagSamsvarsnivaa,
    val kommentarBrudd: String?,
)
