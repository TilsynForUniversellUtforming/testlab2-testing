package no.uutilsynet.testlab2testing.ekstern.resultat.model

import java.time.Instant
import no.uutilsynet.testlab2.constants.Kontrolltype

data class VerksemdEkstern(val namn: String, val organisasjonsnummer: String)

data class TestEkstern(
    val rapportId: String,
    val loeysingId: Int,
    val loeysingNamn: String,
    val organisasjonsnamn: String,
    val organisasjonsnummer: String,
    val score: Double,
    val kontrollType: Kontrolltype,
    val kontrollNamn: String,
    val utfoert: Instant
)

data class TestListElementEkstern(val verksemd: VerksemdEkstern, val testList: List<TestEkstern>)
