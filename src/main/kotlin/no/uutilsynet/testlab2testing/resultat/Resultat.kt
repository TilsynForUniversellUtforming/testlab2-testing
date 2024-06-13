package no.uutilsynet.testlab2testing.resultat

import java.time.LocalDate
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Kontroll

data class Resultat(
    val id: Int,
    val namn: String,
    val type: Kontroll.Kontrolltype,
    val dato: LocalDate,
    val loeysingar: List<LoeysingResultat>
)

data class LoeysingResultat(
    val loeysingId: Int,
    val loeysingNamn: String,
    val verksemdNamn: String,
    val score: Double,
    val testType: TestgrunnlagType,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testar: List<String>,
    val progresjon: Int = 0
)

data class ResultatLoeysing(
    val id: Int,
    val namn: String,
    val typeKontroll: Kontroll.Kontrolltype,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val testar: List<String>,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testregelId: Int,
    val kravId: Int?,
    val kravTittel: String?,
)

/** Uttrekk resultat per løysing */
data class ResultatOversiktLoeysing(
    val loeysingId: Int,
    val loeysingNamn: String,
    val typeKontroll: Kontroll.Kontrolltype,
    val kontrollNamn: String,
    val testar: List<String>,
    val score: Double,
    val kravId: Int,
    val kravTittel: String,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
)

data class ResultatTema(
    val temaNamn: String,
    val score: Int,
    val talTestaElement: Int,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
    val talIkkjeTestbar: Int,
    val talIkkjeForekomst: Int,
)
