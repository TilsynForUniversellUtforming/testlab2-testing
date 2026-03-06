package no.uutilsynet.testlab2testing.resultat.export

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelExport
import org.springframework.stereotype.Service

@Service
class ResultatExportMapper(
    val aggregeringDAO: AggregeringDAO,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val maalingDAO: MaalingDAO
) {

    fun getAggregeringForTestgrunnlag(testgrunnlagId: Int): List<AggregeringPerTestregelExport> {
        return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)
        .map { it.toAggregeringPerTestregelExport(getTestrunIdForTestgrunnlag(testgrunnlagId)) }
    }

    fun getAggregeringForMaaling(maalingId: Int): List<AggregeringPerTestregelExport> {
        return aggregeringDAO.getAggregertResultatTestregelForMaaling(maalingId)
            .map { it.toAggregeringPerTestregelExport(getTestrunIdforMaaling(maalingId)) }
    }


    fun getTestrunIdForTestgrunnlag(testgrunnlagId: Int): String {
        return testgrunnlagDAO.getTestrunUuidForTestgrunnlag(testgrunnlagId).getOrThrow()
    }

    fun getTestrunIdforMaaling(maalingId: Int): String {
        return maalingDAO.getTestrunUuidForMaaling(maalingId).getOrThrow()
    }
}

private fun AggregeringPerTestregelDB.toAggregeringPerTestregelExport(
    testrunUuid: String
): AggregeringPerTestregelExport {
    return AggregeringPerTestregelExport(
        testrunUuid = testrunUuid,
        loeysingId = this.loeysingId,
        testregelId = this.testregelId,
        suksesskriterium = this.suksesskriterium,
        fleireSuksesskriterium = this.fleireSuksesskriterium,
        talElementSamsvar = this.talElementSamsvar,
        talElementBrot = this.talElementBrot,
        talElementVarsel = this.talElementVarsel,
        talElementIkkjeForekomst = this.talElementIkkjeForekomst,
        talSiderSamsvar = this.talSiderSamsvar,
        talSiderBrot = this.talSiderBrot,
        talSiderIkkjeForekomst = this.talSiderIkkjeForekomst,
        testregelGjennomsnittlegSideSamsvarProsent = this.testregelGjennomsnittlegSideSamsvarProsent,
        testregelGjennomsnittlegSideBrotProsent = this.testregelGjennomsnittlegSideBrotProsent
    )
}
