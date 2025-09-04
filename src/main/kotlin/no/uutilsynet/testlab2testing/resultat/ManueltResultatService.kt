package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.common.Constants
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime

@Service
class ManueltResultatService(
    resultatDAO: ResultatDAO,
    kravregisterClient: KravregisterClient,
    testregelService: TestregelService,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val bildeService: BildeService,
) : KontrollResultatService(resultatDAO, kravregisterClient, testregelService) {



    override fun getResultatForKontroll(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int,
    ): List<TestresultatDetaljert> {
        return getFilteredAndMappedResults(kontrollId, loeysingId) {
            filterByTestregel(it.testregelId, listOf(testregelId)) && it.elementResultat != null
        }
    }

    override fun getResultatForKontroll(
        kontrollId: Int,
        loeysingId: Int,
    ): List<TestresultatDetaljert> {
        return getFilteredAndMappedResults(kontrollId, loeysingId) { true }
    }

    fun getFilteredAndMappedResults(
        kontrollId: Int,
        loeysingId: Int,
        filter: (ResultatManuellKontroll) -> Boolean
    ): List<TestresultatDetaljert> {
        val testresultat = getTestresultatForKontroll(kontrollId, loeysingId)
        val sideutvalIdUrlMap = getSideutvalMap(testresultat)
        return testresultat
            .filter(filter)
            .map { resultatManuellKontrollTotestresultatDetaljert(it, sideutvalIdUrlMap) }
    }

    fun getTestresultatForKontroll(
        kontrollId: Int,
        loeysingId: Int,
    ): List<ResultatManuellKontroll> {
        val testgrunnlag = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest
        return getResultatPrTestgrunnlag(testgrunnlag.id)
            .filter { it.loeysingId == loeysingId }
    }

    fun getResultatPrTestgrunnlag(testgrunnlagId: Int) =
        testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()

    private fun getSideutvalMap(testresultat: List<ResultatManuellKontroll>): Map<Int, URL> {
        val sideutvalIds = testresultat.map { it.sideutvalId }.distinct()
        return sideutvalDAO.getSideutvalUrlMapKontroll(sideutvalIds)
    }

    private fun resultatManuellKontrollTotestresultatDetaljert(
        it: ResultatManuellKontroll,
        sideutvalIdUrlMap: Map<Int, URL>,
    ): TestresultatDetaljert {
        val testregel: Testregel = getTesteregelFromId(it.testregelId)
        return TestresultatDetaljert(
            it.id,
            it.loeysingId,
            it.testregelId,
            testregel.testregelId,
            it.testgrunnlagId,
            getUrlFromSideutval(sideutvalIdUrlMap, it),
            getSuksesskriteriumFromTestregel(testregel.kravId),
            testVartUtfoertToLocalTime(it.testVartUtfoert),
            it.elementUtfall,
            it.elementResultat,
            TestresultatDetaljert.ElementOmtale(
                htmlCode = null, pointer = null, description = it.elementOmtale
            ),
            it.brukar,
            it.kommentar,
            getBildeForTestresultat(it)
        )
    }

    private fun getUrlFromSideutval(
        sideutvalIdUrlMap: Map<Int, URL>,
        it: ResultatManuellKontroll,
    ): URL {
        return sideutvalIdUrlMap[it.sideutvalId]
            ?: throw IllegalArgumentException("Ugyldig testresultat url manglar")
    }

    private fun getBildeForTestresultat(it: ResultatManuellKontroll) =
        bildeService.listBildeForTestresultat(it.id).getOrNull()

    private fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
        return testVartUtfoert?.atZone(Constants.ZONEID_OSLO)?.toLocalDateTime()
    }


    override fun getAlleResultat(): List<ResultatLoeysingDTO> {
        return resultatDAO.getTestresultatTestgrunnlag()
    }

    override fun progresjonPrLoeysing(
        testgrunnlagId: Int,
        loeysingar: ResultatService.LoysingList,
    ): Map<Int, Int> {

        return getResultatPrTestgrunnlag(testgrunnlagId)
                .groupBy { it.loeysingId }
                .entries
                .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
                .associateBy({ it.first }, { it.second })
    }

    private fun percentageFerdig(result: List<ResultatManuellKontroll>): Int =
        (result
            .map { it.status }
            .count { it == ResultatManuellKontrollBase.Status.Ferdig }
            .toDouble() / result.size)
            .times(100)
            .toInt()

    override fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
        val testgrunnlagId = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest.id
        val resultatTestgrunnlag = testgrunnlagId.let { resultatDAO.getTestresultatTestgrunnlag(it) }

        return resultatTestgrunnlag
    }

    override fun getBrukararForTest(kontrollId: Int): List<String> {
        return testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).let { testgrunnlag ->
            getTestgrunnlagIds(testgrunnlag).map { testgrunnlagId ->
                return testResultatDAO
                    .getBrukararForTestgrunnlag(testgrunnlagId)
                    .getOrThrow()
                    .map { it.namn }
                    .distinct()
            }
        }
    }

    private fun getTestgrunnlagIds(it: TestgrunnlagList) =
        listOf(it.opprinneligTest.id) + it.restestar.map { it.id }
}
