package no.uutilsynet.testlab2testing.inngaendekontroll

import no.uutilsynet.testlab2testing.testregel.Testregel

data class Sak(
    val virksomhet: String,
    val loeysingar: List<Loeysing> = emptyList(),
    val testreglar: List<Testregel> = emptyList()
) {
  data class Loeysing(val loeysingId: Int, val nettsider: List<Nettside> = emptyList())

  data class Nettside(
      val type: String,
      val url: String,
      val beskrivelse: String,
      val begrunnelse: String
  )
}
