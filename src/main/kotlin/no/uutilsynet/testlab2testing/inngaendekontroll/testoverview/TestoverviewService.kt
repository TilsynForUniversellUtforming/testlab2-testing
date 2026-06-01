package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Loeysingstype
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.newestTestgrunnlagIds
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase.Status
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataListElement
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataService
import org.springframework.stereotype.Service

@Suppress("LongParameterList")
@Service
class TestoverviewService(
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val statisticsService: TestOverviewStatisticsService,
    val styringsdataService: StyringsdataService,
    val testgrunnlagService: TestgrunnlagService,
    val testResultatDAO: TestResultatDAO,
    val kontrollDAO: KontrollDAO,
) {


    fun listTestOverviewElements(kontrollId: Int): List<TestingStatus> {
        val kontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().single()
        val testgrunnlagList = testgrunnlagService.getTestgrunnlagForKontroll(kontrollId).toList()
        val loeysingar = getLoeysingMapFromTestgrunnlag(testgrunnlagList)
        val allResultat = testResultatDAO.getManyResultsByKontrollId(kontrollId).getOrThrow()
        val styringsdataMap = styringsdataService.getStyringsdataMapForKontroll(kontrollId)

        return testgrunnlagList.flatMap { testgrunnlagKontroll ->
            processTestgrunnlagKontroll(
                testgrunnlagKontroll,
                loeysingar,
                kontroll.kontrolltype,
                allResultat,
                styringsdataMap,
                testgrunnlagList.newestTestgrunnlagIds()
            )
        }
    }


    private fun processTestgrunnlagKontroll(
        testgrunnlagKontroll: TestgrunnlagKontroll,
        loeysingarMap: Map<Int, Loeysing>,
        kontrolltype: Kontrolltype,
        allResultat: Map<Int, List<ResultatManuellKontroll>>,
        styringsdataMap: Map<Int, StyringsdataListElement>,
        newestTestgrunnlagIds: Set<Int>
    ): List<TestingStatus> {
        val testresultat = allResultat[testgrunnlagKontroll.id] ?: emptyList()
        val testresultatByLoeysing = testresultat.groupBy { it.loeysingId }
        val isNewest = testgrunnlagKontroll.id in newestTestgrunnlagIds

        return mapSideutvalToLoeysing(testgrunnlagKontroll, loeysingarMap).map { loeysing ->
            mapToTestingStatus(
                loeysing,
                testgrunnlagKontroll,
                kontrolltype,
                testresultatByLoeysing[loeysing.id] ?: emptyList(),
                styringsdataMap[loeysing.id],
                isNewest
            )
        }

    }


    private fun mapSideutvalToLoeysing(
        testgrunnlagKontroll: TestgrunnlagKontroll,
        loeysingarMap: Map<Int, Loeysing>
    ): List<Loeysing> = testgrunnlagKontroll.sideutval.mapNotNull { sideutval -> loeysingarMap[sideutval.loeysingId] }


    private fun getLoeysingMapFromTestgrunnlag(testgrunnlagList: List<TestgrunnlagKontroll>): Map<Int, Loeysing> {
        val loeysingIds = testgrunnlagList.flatMap { it.sideutval }.map { it.loeysingId }
        return loeysingsRegisterClient.getMany(loeysingIds).getOrThrow().associateBy { it.id }
    }

    private fun mapToTestingStatus(
        loeysing: Loeysing,
        testgrunnlagKontroll: TestgrunnlagKontroll,
        kontrollType: Kontrolltype,
        resultat: List<ResultatManuellKontroll>,
        styringsdata: StyringsdataListElement?,
        isNewest: Boolean
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
            styringdataStatus = styringsdataStatus(styringsdata),
            styringsdataId = styringsdata?.id,
            kanReteste = kanReteste(resultat, isNewest)
        )
    }


    private fun getTeststatus(results: List<ResultatManuellKontroll>): ManuellTestStatus {
        return when {
            results.all { it.status == Status.IkkjePaabegynt } -> ManuellTestStatus.IKKJE_STARTA
            results.all { it.status == Status.Ferdig } -> ManuellTestStatus.FERDIG
            else -> ManuellTestStatus.UNDER_ARBEID
        }
    }

    private fun styringsdataStatus(styringsdata: StyringsdataListElement?): StyringsdataStatus {
        return when {
            styringsdata == null -> StyringsdataStatus.INGEN_REAKSJON_BRUKT
            styringsdata.isBot -> StyringsdataStatus.BOT
            styringsdata.isPaalegg -> StyringsdataStatus.PAALEG
            else -> StyringsdataStatus.INGEN_REAKSJON_BRUKT
        }
    }

    private fun kanSlette(resultat: List<ResultatManuellKontroll>, testgrunnlagType: TestgrunnlagType): Boolean {
        return resultat.isEmpty() && testgrunnlagType == TestgrunnlagType.RETEST
    }

    private fun kanReteste(resultat: List<ResultatManuellKontroll>, isNewest: Boolean): Boolean {
        val harBrot = resultat.any { it.elementResultat == TestresultatUtfall.brot && it.status == Status.Ferdig }
        return harBrot && isNewest
    }

}









