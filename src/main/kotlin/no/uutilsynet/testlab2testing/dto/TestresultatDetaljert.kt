package no.uutilsynet.testlab2testing.dto

import java.net.URL
import java.time.LocalDateTime
import no.uutilsynet.testlab2testing.brukar.Brukar

data class TestresultatDetaljert(
    val resultatId: Int?, // databaseId
    val loeysingId: Int,
    val testregelId: Int,
    val testregelNoekkel: String,
    val testgrunnlagId: Int,
    val side: URL,
    val suksesskriterium: List<String>,
    val testVartUtfoert: LocalDateTime?,
    val elementUtfall: String?,
    val elementResultat: TestresultatUtfall?,
    val elementOmtale: ElementOmtale?,
    val brukarId: Brukar?,
    val kommentar: String? = ""
) {

  data class ElementOmtale(val htmlCode: String?, val pointer: String?, val description: String?)
}
