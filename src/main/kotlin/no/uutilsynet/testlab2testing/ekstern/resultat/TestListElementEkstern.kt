package no.uutilsynet.testlab2testing.ekstern.resultat

import Kontrolltype
import java.time.Instant

data class VerksemdEkstern(val namn: String, val organisasjonsnummer: String)

data class TestEkstern(
    val rapportId: String,
    val loeysingNamn: String,
    val score: Double,
    val kontrollType: Kontrolltype,
    val publisert: Instant
)

data class TestListElementEkstern(val verksemd: VerksemdEkstern, val testList: List<TestEkstern>)
