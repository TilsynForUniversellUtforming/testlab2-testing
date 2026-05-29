package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Loeysingstype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase.Status
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataListElement
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataService
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.springframework.stereotype.Service

@Suppress("LongParameterList")
@Service
class TestoverviewService(
    val testregelClient: TestregelClient,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val statisticsService: TestOverviewStatisticsService,
    val styringsdataService: StyringsdataService,
    val testgrunnlagService: TestgrunnlagService,
    val testResultatDAO: TestResultatDAO,
    val kontrollDAO: KontrollDAO,
) {

    private fun processTestgrunnlagKontroll(
        testgrunnlagKontroll: TestgrunnlagKontroll,
        loeysingarMap: Map<Int, Loeysing>,
        kontrolltype: Kontrolltype,
        resultat: List<ResultatManuellKontroll>
    ): List<TestingStatus> {
        return mapSideutvalToLoeysing(testgrunnlagKontroll, loeysingarMap).map { loeysing ->
            mapToTestingStatus(
                loeysing,
                testgrunnlagKontroll,
                kontrolltype,
                resultat.filter { it.loeysingId == loeysing.id })
        }

    }



    private fun mapSideutvalToLoeysing(
        testgrunnlagKontroll: TestgrunnlagKontroll,
        loeysingarMap: Map<Int, Loeysing>
    ): List<Loeysing> = testgrunnlagKontroll.sideutval.mapNotNull { sideutval -> loeysingarMap[sideutval.loeysingId] }

    fun listTestOverviewElements(kontrollId: Int): List<TestingStatus> {
        val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().single()

        val testgrunnlagList = testgrunnlagService.getTestgrunnlagForKontroll(kontrollId).toList()
        val loeysingar: Map<Int, Loeysing> =
            getLoeysingMapFromTestgrunnlag(testgrunnlagList)

        return testgrunnlagList.flatMap { testgrunnlagKontroll ->
            val testresultat = testResultatDAO.getManyResults(testgrunnlagKontroll.id).getOrThrow()
            processTestgrunnlagKontroll(testgrunnlagKontroll, loeysingar, kontroll.kontrolltype, testresultat)
        }


    }

    private fun getLoeysingMapFromTestgrunnlag(testgrunnlagList: List<TestgrunnlagKontroll>): Map<Int, Loeysing> {
        val loeysingIds = testgrunnlagList.flatMap { it.sideutval }.map { it.loeysingId }

        val loeysingar: Map<Int, Loeysing> =
            loeysingsRegisterClient.getMany(loeysingIds).getOrThrow().associateBy { it.id }
        return loeysingar
    }

    private fun mapToTestingStatus(
        loeysing: Loeysing,
        testgrunnlagKontroll: TestgrunnlagKontroll,
        kontrollType: Kontrolltype,
        resultat: List<ResultatManuellKontroll>
    ): TestingStatus {
        val testregelIdList = testgrunnlagKontroll.testreglar
        val sideutvalIdList = testgrunnlagKontroll.sideutval.toList().map { it.id }
        val testStatistics = statisticsService.getTestingStatusForLoeysing(
            loeysing.id,
            testgrunnlagKontroll.id,
            resultat,
            testregelIdList,
            sideutvalIdList
        )


        return TestingStatus(
            loeysingId = loeysing.id,
            loeysingNamn = loeysing.namn,
            testgrunnlagType = testgrunnlagKontroll.type,
            loeysingstype = Loeysingstype.NETT,
            kontrollType = kontrollType,
            teststatistics = testStatistics,
            status = getTeststatus(resultat),
            kanSlette = kanSlette(resultat, testgrunnlagKontroll.type),
            styringdataStatus = styringsdataStatus(loeysing.id, testgrunnlagKontroll.id)
        )
    }

    private fun getTeststatus(results: List<ResultatManuellKontroll>): ManuellTestStatus {
        return when {
            results.all { it.status == Status.IkkjePaabegynt } -> ManuellTestStatus.IKKJE_STARTA
            results.all { it.status == Status.Ferdig } -> ManuellTestStatus.FERDIG
            else -> ManuellTestStatus.UNDER_ARBEID
        }
    }

    private fun styringsdataStatus(loeysingId: Int, kontrollId: Int): StyringsdataStatus {
        val styringsdata: StyringsdataListElement? =
            styringsdataService.getStyringsdataForLoeysing(loeysingId, kontrollId)
        
        return when {
            styringsdata == null -> StyringsdataStatus.INGEN_REAKSJON_BRUKT
            styringsdata.isBot  -> StyringsdataStatus.BOT
            styringsdata.isPaalegg -> StyringsdataStatus.PAALEG
            else -> StyringsdataStatus.INGEN_REAKSJON_BRUKT
        }
    }

    fun TestgrunnlagList.toList(): List<TestgrunnlagKontroll> {
        return listOf(this.opprinneligTest) + this.restestar
    }

    private fun kanSlette(resultat: List<ResultatManuellKontroll>, testgrunnlagType: TestgrunnlagType): Boolean {
        return resultat.isEmpty() && testgrunnlagType == TestgrunnlagType.RETEST
    }


}






