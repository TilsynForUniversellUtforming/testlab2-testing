package no.uutilsynet.testlab2testing.kontroll

import java.net.URI

open class SideutvalBase(
    open val loeysingId: Int,
    open val typeId: Int,
    open val begrunnelse: String,
    open val url: URI,
    open val egendefinertType: String?
)

data class Sideutval(
    override val loeysingId: Int,
    override val typeId: Int,
    override val begrunnelse: String,
    override val url: URI,
    override val egendefinertType: String?,
    val id: Int,
) :
    SideutvalBase(
        loeysingId,
        typeId,
        begrunnelse,
        url,
        egendefinertType,
    )
