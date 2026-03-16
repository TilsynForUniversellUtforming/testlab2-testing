package no.uutilsynet.testlab2testing.resultat.common

import no.uutilsynet.testlab2testing.loeysing.Loeysing

class LoysingList(val loeysingar: Map<Int, Loeysing.Expanded>) {
    fun getNamn(loeysingId: Int): String {
        val loeysing = loeysingar[loeysingId]
        return loeysing?.namn ?: ""
    }

    fun getVerksemdNamn(loeysingId: Int): String {
        val loeysing = loeysingar[loeysingId]
        if (loeysing?.verksemd == null) return ""
        return loeysing.verksemd.namn
    }

    fun getOrgnr(loeysingId: Int): String {
        val loeysing = loeysingar[loeysingId]
        if (loeysing?.verksemd == null) return ""
        return loeysing.verksemd.organisasjonsnummer
    }
}