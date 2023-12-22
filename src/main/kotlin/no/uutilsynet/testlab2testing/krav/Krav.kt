package no.uutilsynet.testlab2testing.krav

open class Krav(
    open val id: Int,
    open val tittel: String,
    open val status: String,
    open val innhald: String?,
    open val gjeldAutomat: Boolean,
    open val gjeldNettsider: Boolean,
    open val gjeldApp: Boolean,
    open val urlRettleiing: String?,
)
