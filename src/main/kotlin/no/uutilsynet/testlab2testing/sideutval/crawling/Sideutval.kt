package no.uutilsynet.testlab2testing.sideutval.crawling

import no.uutilsynet.testlab2testing.kontroll.SideutvalType
import java.net.URL

sealed class Sideutval{
    abstract val id:Int
    abstract val adresse:String

        data class Automatisk(override val id: Int,val crawlresultatId:Int, val url:URL) : Sideutval() {
            override val adresse = url.toString()
        }

        data class Manuell(override val id: Int, override val adresse: String, val begrunnelse:String,val sideutvaltype:SideutvalType) : Sideutval()
}