package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.testregel.TestregelDTO

data class SakDTO(
    val id: Int,
    val namn: String,
    val virksomhet: String,
    val ansvarleg: Brukar?,
    val loeysingar: List<Sak.Loeysing> = emptyList(),
    val testreglar: List<TestregelDTO> = emptyList()
) {}
