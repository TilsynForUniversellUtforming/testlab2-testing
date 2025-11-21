package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.testregel.model.Testregel

data class NyttTestgrunnlag(
    val kontrollId: Int,
    val namn: String,
    val type: TestgrunnlagType,
    val sideutval: List<Sideutval>,
    val testregelIdList: List<Int>
)

data class NyttTestgrunnlagFromKontroll(
    val kontrollId: Int,
    val namn: String,
    val type: TestgrunnlagType,
    val sideutval: List<Sideutval>,
    val testregelIdList: List<Testregel>
)
