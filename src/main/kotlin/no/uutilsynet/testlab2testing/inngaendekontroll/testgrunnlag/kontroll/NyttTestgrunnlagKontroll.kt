package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import no.uutilsynet.testlab2testing.kontroll.Sideutval

data class NyttTestgrunnlagKontroll(
    val parentId: Int,
    val namn: String,
    val type: TestgrunnlagKontroll.TestgrunnlagType,
    val sideutval: List<Sideutval>,
    val testregelIdList: List<Int>
)
