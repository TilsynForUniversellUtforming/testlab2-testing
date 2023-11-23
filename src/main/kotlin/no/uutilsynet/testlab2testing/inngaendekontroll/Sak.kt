package no.uutilsynet.testlab2testing.inngaendekontroll

data class Sak(val virksomhet: String, val loeysingar: List<Loeysing> = emptyList()) {
  data class Loeysing(val loeysingId: Int)
}
