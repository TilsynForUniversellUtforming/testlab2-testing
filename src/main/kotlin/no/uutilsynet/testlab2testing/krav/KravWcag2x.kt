package no.uutilsynet.testlab2testing.krav

data class KravWcag2x(
    override val id: Int,
    override val tittel: String,
    override val status: String,
    override val innhald: String?,
    override val gjeldAutomat: Boolean,
    override val gjeldNettsider: Boolean,
    override val gjeldApp: Boolean,
    override val urlRettleiing: String?,
    val prinsipp: String,
    val retningslinje: String,
    val suksesskriterium: String,
    val samsvarsnivaa: WcagSamsvarsnivaa
) :
    Krav(
        id,
        tittel,
        status,
        innhald,
        gjeldAutomat,
        gjeldNettsider,
        gjeldApp,
        urlRettleiing,
    )

enum class WcagSamsvarsnivaa(val nivaa: String) {
  A("A"),
  AA("AA"),
  AAA("AAA")
}
