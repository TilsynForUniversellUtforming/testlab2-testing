package no.uutilsynet.testlab2testing.ekstern.resultat

import java.time.Instant

data class VerksemdEkstern(val namn: String, val organisasjonsnummer: String)

enum class KontrollType {
  INNGAAENDE_KONTROLL,
  FORENKLA_KONTROLL,
  TILSYN,
  STATUSMAALING,
  UTTALESAK,
  ANNA
}

data class TestEkstern(
    val rapportId: String,
    val loeysingNamn: String,
    val score: Int,
    val kontrollType: KontrollType,
    val publisert: Instant
)

data class TestListElementEkstern(val verksemd: VerksemdEkstern, val testList: List<TestEkstern>)
