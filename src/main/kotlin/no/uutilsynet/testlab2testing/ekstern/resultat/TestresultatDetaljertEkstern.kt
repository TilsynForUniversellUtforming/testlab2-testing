package no.uutilsynet.testlab2testing.ekstern.resultat

import java.net.URL
import java.time.LocalDateTime
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.dto.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde

data class TestresultatDetaljertEkstern(
    val testregelNoekkel: String,
    val side: URL,
    val suksesskriterium: List<String>,
    val testVartUtfoert: LocalDateTime?,
    val elementUtfall: String?,
    val elementResultat: TestresultatUtfall?,
    val elementOmtale: TestresultatDetaljert.ElementOmtale?,
    val bilder: List<Bilde>?
)

fun TestresultatDetaljert.toTestresultatDetaljertEkstern() =
    TestresultatDetaljertEkstern(
        testregelNoekkel = this.testregelNoekkel,
        side = this.side,
        suksesskriterium = this.suksesskriterium,
        testVartUtfoert = this.testVartUtfoert,
        elementUtfall = this.elementUtfall,
        elementResultat = this.elementResultat,
        elementOmtale = this.elementOmtale,
        bilder = this.bilder,
    )
