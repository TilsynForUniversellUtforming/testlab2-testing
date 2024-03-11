package no.uutilsynet.testlab2testing.kontroll

data class OpprettetKontroll(
    val id: Int,
    val tittel: String,
    val saksbehandler: String,
    val sakstype: Sakstype,
    val arkivreferanse: String,
) {
  enum class Sakstype {
    Forvaltningssak,
    Arkivsak
  }
}
