package no.uutilsynet.testlab2testing.ekstern.resultat

import java.time.Instant

data class TestListElementDB(
  val eksternTestgrunnlagId: String,
  val kontrollId: Int,
  val loeysingId: Int,
  val kontrollType: KontrollType,
  val publisert: Instant
)

fun TestListElementDB.toListElement(
  loeysingNamn: String,
  score: Int,
): TestEkstern =
  TestEkstern(
    rapportId = this.eksternTestgrunnlagId,
    loeysingNamn = loeysingNamn,
    score = score,
    kontrollType = this.kontrollType,
    publisert = this.publisert
  )