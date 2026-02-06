package no.uutilsynet.testlab2testing.ekstern.resultat.model

import java.net.URL
import java.time.LocalDateTime
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import org.springframework.web.util.UriComponentsBuilder

@Relation(collectionRelation = "testresultat")
data class TestresultatDetaljertEkstern(
    val testregelNoekkel: String,
    val side: URL,
    val suksesskriterium: String,
    val veiledning: URL?,
    val testregelTittel: String,
    val testVartUtfoert: LocalDateTime?,
    val elementUtfall: String?,
    val elementResultat: TestresultatUtfall?,
    val elementOmtale: TestresultatDetaljert.ElementOmtale?,
    val bilder: List<Bilde>?,
) : RepresentationModel<TestresultatDetaljertEkstern>()

fun TestresultatDetaljert.toTestresultatDetaljertEkstern(
    testregel: TestregelAggregate
): TestresultatDetaljertEkstern =
    TestresultatDetaljertEkstern(
        testregelNoekkel = this.testregelNoekkel,
        side = this.side,
        suksesskriterium = testregel.krav.tittel,
        veiledning = testregel.krav.urlRettleiing,
        testregelTittel = testregel.namn,
        testVartUtfoert = this.testVartUtfoert,
        elementUtfall = this.elementUtfall,
        elementResultat = this.elementResultat,
        elementOmtale =
            this.elementOmtale?.copy(
                htmlCode = null,
                pointer = this.elementOmtale.pointer,
                description = this.elementOmtale.description),
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
