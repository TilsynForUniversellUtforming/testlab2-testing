package no.uutilsynet.testlab2testing.rapport

import java.net.URI

data class Rapport(
    val rapportNummer: String,
    val datoFra: String?,
    val datoTil: String?,
    val verksemd: String,
    val loeysing: String,
    val avvik: List<Avvik>,
)

data class Avvik(
    val nummer: Int?,
    val resultatId: Int?,
    val testregel: TestregelRapport?,
    val side: Side,
    val elementOmtale: String,
    val elementResultat: String?,
    val elementUtfall: String?,
    val tema: String?,
)

data class TestregelRapport(
    val testregelId: Int,
    val testregelNoekkel: String,
    val kravId: Int,
    val kravTittel: String,
    val kravUrl: URI?
)

data class Side(val sideNr: Int, val sideTittel: String, val sideUrl: URI)

data class SuksesskriteriumBrudd(val suksesskriterium: String, val lenke: URI)
