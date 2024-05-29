package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Sideutval

data class NyttTestgrunnlag(
    val kontrollId: Int,
    val namn: String,
    val type: TestgrunnlagType,
    val sideutval: List<Sideutval>,
    val testregelIdList: List<Int>
)
