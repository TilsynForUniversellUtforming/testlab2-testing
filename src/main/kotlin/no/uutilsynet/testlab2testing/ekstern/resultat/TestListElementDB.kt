package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.resultat.LoeysingResultat
import java.time.Instant

data class TestListElementDB(
    val eksternTestgrunnlagId: String,
    val kontrollId: Int,
    val loeysingId: Int,
    val kontrollType: Kontrolltype,
    val kontrollNamn: String,
    val publisert: Instant,
    val utfoert: Instant,
)

fun TestListElementDB.toListElement(
    loeysing: LoeysingResultat,
    score: Double,
): TestEkstern =
    TestEkstern(
        rapportId = this.eksternTestgrunnlagId,
        loeysingId = this.loeysingId,
        loeysingNamn = loeysing.loeysingNamn,
        organisasjonsnamn = loeysing.verksemdNamn,
        score = score,
        kontrollType = this.kontrollType,
        kontrollNamn = this.kontrollNamn,
        utfoert = this.utfoert)
