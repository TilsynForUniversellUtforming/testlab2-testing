package no.uutilsynet.testlab2testing.ekstern.resultat.model

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import org.springframework.web.util.UriComponentsBuilder
import java.net.URL
import java.time.LocalDateTime

@Relation(collectionRelation = "testresultat")
data class TestresultatDetaljertEkstern(
    val testregelNoekkel: String,
    val side: URL,
    val suksesskriterium: String,
    val testregelTittel: String,
    val testVartUtfoert: LocalDateTime?,
    val elementUtfall: String?,
    val elementResultat: TestresultatUtfall?,
    val elementOmtale: TestresultatDetaljert.ElementOmtale?,
    val bilder: List<Bilde>?,
) : RepresentationModel<TestresultatDetaljertEkstern>()

fun TestresultatDetaljert.toTestresultatDetaljertEkstern(
    testregel: TestregelKrav
): TestresultatDetaljertEkstern =
    TestresultatDetaljertEkstern(
        testregelNoekkel = this.testregelNoekkel,
        side = this.side,
        suksesskriterium = testregel.krav.tittel,
        testregelTittel = testregel.namn,
        testVartUtfoert = this.testVartUtfoert,
        elementUtfall = this.elementUtfall,
        elementResultat = this.elementResultat,
        elementOmtale = this.elementOmtale?.copy(
            htmlCode = null,
            pointer = this.elementOmtale.pointer,
            description = this.elementOmtale.description
        ),
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
