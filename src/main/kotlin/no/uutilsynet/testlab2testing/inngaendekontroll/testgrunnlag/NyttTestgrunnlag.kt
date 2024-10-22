package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.kontroll.SideutvalElement

data class NyttTestgrunnlag(
    val kontrollId: Int,
    val namn: String,
    val type: TestgrunnlagType,
    val sideutval: List<SideutvalElement>,
    val testregelIdList: List<Int>
)
