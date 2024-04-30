package no.uutilsynet.testlab2testing.resultat

import java.util.*
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.Testgrunnlag
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import java.time.LocalDate

data class Resultat(
    val id: Int,
    val namn: String,
    val type: Kontroll.KontrollType,
    val testar: String,
    val dato: LocalDate,
    val loeysingar: List<LoeysingResultat>
)

data class LoeysingResultat(
    val id: Int,
    val namn: String,
    val score: Double,
    val testType: Testgrunnlag.TestgrunnlagType
)

data class ResultatLoeysing(
    val id: Int,
    val namn: String,
    val typeKontroll: Kontroll.KontrollType,
    val testType: Testgrunnlag.TestgrunnlagType,
    val dato: LocalDate,
    val testar: String,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int
)
