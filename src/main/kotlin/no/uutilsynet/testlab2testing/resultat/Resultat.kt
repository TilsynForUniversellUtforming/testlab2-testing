package no.uutilsynet.testlab2testing.resultat

import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType

data class Resultat(
    val id: Int,
    val namn: String,
    val type: Kontrolltype,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val publisert: Boolean,
    val loeysingar: List<LoeysingResultat>
)

data class LoeysingResultat(
    val loeysingId: Int,
    val loeysingNamn: String,
    val verksemdNamn: String,
    val organisasjonsnummer: String,
    val score: Double,
    val testType: TestgrunnlagType,
    val talTestaElement: Int?,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testar: List<String>,
    val progresjon: Int = 0
)

data class ResultatLoeysing(
    val id: Int,
    val testgrunnlagId: Int,
    val namn: String,
    val typeKontroll: Kontrolltype,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val testar: List<String>,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testregelId: Int,
    val testregeltTittel: String,
    val kravId: Int,
    val kravTittel: String,
)

data class ResultatLoeysingDTO(
    val id: Int,
    val testgrunnlagId: Int,
    val namn: String,
    val typeKontroll: Kontrolltype,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val testar: List<String>,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testregelId: Int,
)

/** Uttrekk resultat per løysing */
data class ResultatOversiktLoeysing(
    val loeysingId: Int,
    val loeysingNamn: String,
    val typeKontroll: Kontrolltype,
    val kontrollNamn: String,
    val testar: List<String>,
    val score: Double?,
    val testregelId: Int,
    val testregelTittel: String,
    val talTestaElement: Int,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
)

data class ResultatTema(
    val temaNamn: String,
    val score: Double?,
    val talTestaElement: Int,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
    val talVarsel: Int,
    val talElementIkkjeForekomst: Int,
)

data class ResultatKrav(
    val kravId: Int,
    val suksesskriterium: String,
    val score: Double?,
    val talTestaElement: Int,
    val talElementBrot: Int,
    val talElementSamsvar: Int,
    val talElementVarsel: Int,
    val talElementIkkjeForekomst: Int,
)

data class ResultatKravBase(
    val kravId: Int,
    val score: Double = 0.0,
    val talElementBrot: Int = 0,
    val talElementSamsvar: Int = 0,
    val talElementVarsel: Int = 0,
    val talElementIkkjeForekomst: Int = 0,
)
