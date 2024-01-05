package no.uutilsynet.testlab2testing.inngaendekontroll

import no.uutilsynet.testlab2testing.testregel.TestregelDTO

data class SakDTO(
    val virksomhet: String,
    val loeysingar: List<Sak.Loeysing> = emptyList(),
    val testreglar: List<TestregelDTO> = emptyList()
) {}
