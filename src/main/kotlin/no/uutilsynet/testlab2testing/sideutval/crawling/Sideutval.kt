package no.uutilsynet.testlab2testing.sideutval.crawling

import java.net.URL
import no.uutilsynet.testlab2testing.kontroll.SideutvalType

sealed class Sideutval {
  abstract val id: Int
  abstract val adresse: String

  data class Automatisk(override val id: Int, val crawlresultatId: Int, val url: URL) :
      Sideutval() {
    override val adresse = url.toString()
  }

  data class Manuell(
      override val id: Int,
      override val adresse: String,
      val begrunnelse: String,
      val sideutvaltype: SideutvalType
  ) : Sideutval()
}
