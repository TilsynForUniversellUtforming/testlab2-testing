package no.uutilsynet.testlab2testing.testregel.krav

import no.uutilsynet.testlab2.constants.KravStatus
import no.uutilsynet.testlab2.constants.WcagPrinsipp
import no.uutilsynet.testlab2.constants.WcagRetninglinje
import no.uutilsynet.testlab2.constants.WcagSamsvarsnivaa
import java.net.URL

data class KravWcag2x(
    val id: Int,
    val tittel: String,
    val status: KravStatus,
    val innhald: String?,
    val gjeldAutomat: Boolean,
    val gjeldNettsider: Boolean,
    val gjeldApp: Boolean,
    val urlRettleiing: URL?,
    val prinsipp: WcagPrinsipp,
    val retningslinje: WcagRetninglinje,
    val suksesskriterium: String,
    val samsvarsnivaa: WcagSamsvarsnivaa,
    val kommentarBrudd: String?,
)
