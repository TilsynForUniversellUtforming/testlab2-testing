package no.uutilsynet.testlab2testing.inngaendekontroll

data class Sak(val virksomhet: String, val loeysingar: List<Loeysing> = emptyList()) {
  data class Loeysing(val loeysingId: Int, val nettsider: List<Nettside> = emptyList())

  data class Nettside(
      val type: String,
      val url: String,
      val beskrivelse: String,
      val begrunnelse: String
  )
}
