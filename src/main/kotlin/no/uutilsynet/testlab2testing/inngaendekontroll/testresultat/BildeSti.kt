package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URI
import java.time.Instant

open class BildeBase(
    open val id: Int,
    open val opprettet: Instant,
)

data class BildeSti(
    override val id: Int,
    val bilde: String,
    val thumbnail: String,
    override val opprettet: Instant,
) : BildeBase(id, opprettet)

data class Bilde(
    override val id: Int,
    val bildeURI: URI,
    val thumbnailURI: URI,
    override val opprettet: Instant
) : BildeBase(id, opprettet)

data class BildeListItem(
    val bildeURI: URI,
    val thumbnailURI: URI,
)

fun Bilde.toBildeListItem() = BildeListItem(bildeURI, thumbnailURI)
