package no.uutilsynet.testlab2testing.resultat

import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import no.uutilsynet.testlab2testing.common.Constants
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Service

@Service
class ManueltResultatService(
    resultatDAO: ResultatDAO,
    testregelDAO: TestregelDAO,
    kravregisterClient: KravregisterClient,
    val testgrunnlagDAO: TestgrunnlagDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val bildeService: BildeService
) : KontrollResultatService(resultatDAO, testregelDAO, kravregisterClient) {

  fun getResulatForManuellKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int
  ): List<TestresultatDetaljert> {
    val testresultat = getTestresultatForKontroll(kontrollId, loeysingId)

    val sideutvalIdUrlMap: Map<Int, URL> = getSideutvalMap(testresultat)

    return testresultat
        .map { resultatManuellKontrollTotestresultatDetaljert(it, sideutvalIdUrlMap) }
        .filter { filterByTestregel(it.testregelId, listOf(testregelId)) }
        .filter { it.elementResultat != null }
  }

  private fun getTestresultatForKontroll(
      kontrollId: Int,
      loeysingId: Int
  ): List<ResultatManuellKontroll> {
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest
    val testresultat =
        getResultatPrTestgrunnlag(testgrunnlag.id).filter { it.loeysingId == loeysingId }
    return testresultat
  }

  fun getResultatPrTestgrunnlag(testgrunnlagId: Int) =
      testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()

  private fun resultatManuellKontrollTotestresultatDetaljert(
      it: ResultatManuellKontroll,
      sideutvalIdUrlMap: Map<Int, URL>
  ): TestresultatDetaljert {
    val testregel: Testregel = getTesteregelFromId(it.testregelId)
    // it.testregel er databaseId ikkje feltet testregelId i db
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
            htmlCode = null, pointer = null, description = it.elementOmtale),
        it.brukar,
        it.kommentar,
        getBildeForTestresultat(it))
  }

  private fun getBildeForTestresultat(it: ResultatManuellKontroll) =
      bildeService.listBildeForTestresultat(it.id).getOrNull()

  private fun getUrlFromSideutval(
      sideutvalIdUrlMap: Map<Int, URL>,
      it: ResultatManuellKontroll
  ): URL {
    val url = sideutvalIdUrlMap[it.sideutvalId]
    requireNotNull(url) { "Ugyldig testresultat url manglar" }
    return url
  }

  private fun getSideutvalMap(testresultat: List<ResultatManuellKontroll>): Map<Int, URL> {
    val sideutvalIds = getSideutvalIds(testresultat)
    val sideutvalIdUrlMap: Map<Int, URL> = sideutvalDAO.getSideutvalUrlMapKontroll(sideutvalIds)
    return sideutvalIdUrlMap
  }

  private fun getSideutvalIds(testresultat: List<ResultatManuellKontroll>) =
      testresultat.map { it.sideutvalId }.distinct()

  private fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
    return testVartUtfoert?.atZone(Constants.ZONEID_OSLO)?.toLocalDateTime()
  }

  fun getKontrollResultatAlleTestgrunnlag(): List<ResultatLoeysingDTO> {
    return resultatDAO.getTestresultatTestgrunnlag()
  }

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
