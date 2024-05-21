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
    val id: Int,
    val namnLoeysing: String,
    val score: Double,
    val testType: TestgrunnlagType,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testar: String,
    val progresjon: Int = 0
)

data class ResultatLoeysing(
    val id: Int,
    val namn: String,
    val typeKontroll: Kontroll.Kontrolltype,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val testar: String,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int
)
