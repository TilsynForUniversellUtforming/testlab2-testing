package no.uutilsynet.testlab2testing.kontroll

import java.net.URI

open class SideutvalElementBase(
    open val loeysingId: Int,
    open val typeId: Int,
    open val begrunnelse: String,
    open val url: URI,
    open val egendefinertType: String?
)

data class SideutvalElement(
    override val loeysingId: Int,
    override val typeId: Int,
    override val begrunnelse: String,
    override val url: URI,
    override val egendefinertType: String?,
    val id: Int,
) :
    SideutvalElementBase(
        loeysingId,
        typeId,
        begrunnelse,
        url,
        egendefinertType,
    )
