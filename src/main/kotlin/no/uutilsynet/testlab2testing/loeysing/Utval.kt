package no.uutilsynet.testlab2testing.loeysing

data class Utval(val id: Int, val namn: String, val loeysingar: List<Loeysing>)

data class UtvalListItem(val id: UtvalId, val namn: String)
