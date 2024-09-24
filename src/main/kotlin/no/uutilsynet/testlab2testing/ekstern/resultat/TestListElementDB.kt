package no.uutilsynet.testlab2testing.ekstern.resultat

import java.time.Instant
import no.uutilsynet.testlab2.constants.Kontrolltype

data class TestListElementDB(
    val eksternTestgrunnlagId: String,
    val kontrollId: Int,
    val loeysingId: Int,
    val kontrollType: Kontrolltype,
    val publisert: Instant
)

fun TestListElementDB.toListElement(
    loeysingNamn: String,
    score: Double,
): TestEkstern =
    TestEkstern(
        rapportId = this.eksternTestgrunnlagId,
        loeysingNamn = loeysingNamn,
        score = score,
        kontrollType = this.kontrollType,
        publisert = this.publisert)
