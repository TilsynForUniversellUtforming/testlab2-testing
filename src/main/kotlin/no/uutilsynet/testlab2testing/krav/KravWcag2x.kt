package no.uutilsynet.testlab2testing.krav

data class KravWcag2x(
    val id: Int,
    val tittel: String,
    val status: String,
    val innhald: String?,
    val gjeldAutomat: Boolean,
    val gjeldNettsider: Boolean,
    val gjeldApp: Boolean,
    val urlRettleiing: String?,
    val prinsipp: String,
    val retningslinje: String,
    val suksesskriterium: String,
    val samsvarsnivaa: WcagSamsvarsnivaa
)

enum class WcagSamsvarsnivaa() {
  A,
  AA,
  AAA
}
