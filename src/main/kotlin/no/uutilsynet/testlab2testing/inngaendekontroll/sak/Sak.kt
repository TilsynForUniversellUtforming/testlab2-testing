package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import no.uutilsynet.testlab2testing.testregel.Testregel

data class Sak(
    val id: Int,
    val namn: String,
    val virksomhet: String,
    val loeysingar: List<Loeysing> = emptyList(),
    val testreglar: List<Testregel> = emptyList()
) {
  data class Loeysing(val loeysingId: Int, val nettsider: List<Nettside> = emptyList())

  data class Nettside(
      val id: Int,
      val type: String,
      val url: String,
      val beskrivelse: String,
      val begrunnelse: String
  )
}

/** Ein enklare versjon av Sak-klassen som brukast i lister. */
data class SakListeElement(val id: Int, val virksomhet: String)
