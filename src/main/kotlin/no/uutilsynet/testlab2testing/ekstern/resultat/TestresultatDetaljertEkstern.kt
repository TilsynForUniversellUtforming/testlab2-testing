package no.uutilsynet.testlab2testing.ekstern.resultat

import java.net.URL
import java.time.LocalDateTime
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.testregel.Testregel
import org.springframework.web.util.UriComponentsBuilder

data class TestresultatDetaljertEkstern(
    val testregelNoekkel: String,
    val side: URL,
    val suksesskriterium: List<String>,
    val testregelTittel: String,
    val testVartUtfoert: LocalDateTime?,
    val elementUtfall: String?,
    val elementResultat: TestresultatUtfall?,
    val elementOmtale: TestresultatDetaljert.ElementOmtale?,
    val bilder: List<Bilde>?
)

fun TestresultatDetaljert.toTestresultatDetaljertEkstern(testregel: Testregel) =
    TestresultatDetaljertEkstern(
        testregelNoekkel = this.testregelNoekkel,
        side = this.side,
        suksesskriterium = this.suksesskriterium,
        testregelTittel = testregel.namn,
        testVartUtfoert = this.testVartUtfoert,
        elementUtfall = this.elementUtfall,
        elementResultat = this.elementResultat,
        elementOmtale = this.elementOmtale,
        bilder = this.bilder?.map { it.toEksternPath() },
    )

fun Bilde.toEksternPath(): Bilde {
  return this.copy(
      bildeURI =
          UriComponentsBuilder.fromUri(this.bildeURI)
              .replacePath("ekstern/tester/${this.bildeURI.path}")
              .build()
              .toUri(),
      thumbnailURI =
          UriComponentsBuilder.fromUri(this.thumbnailURI)
              .replacePath("ekstern/tester/${this.thumbnailURI.path}")
              .build()
              .toUri())
}
