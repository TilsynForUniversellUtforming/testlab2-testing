package no.uutilsynet.testlab2testing.resultat

import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.*
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontrollDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class ResultatService(
    val maalingResource: MaalingResource,
    val testregelDAO: TestregelDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val kravregisterClient: KravregisterClient,
    val resultatDAO: ResultatDAO,
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val kontrollDAO: KontrollDAO,
    val testgrunnlagDao: TestgrunnlagKontrollDAO,
    val bildeService: BildeService
) {

  fun getResultatForAutomatiskKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): List<TestresultatDetaljert> {
    val maalingId =
        maalingDAO.getMaalingIdFromKontrollId(kontrollId)
            ?: throw RuntimeException("Fant ikkje maalingId for kontrollId $kontrollId")
    val testregelIds: List<Int> = testregelDAO.getTestregelForKrav(kravId).map { it.id }

    val testresultat: List<AutotesterTestresultat> =
        maalingResource.getTestresultat(maalingId, loeysingId).getOrThrow()

    if (testresultat.isNotEmpty() && testresultat.first() is TestResultat) {
      return testresultat
          .map { it as TestResultat }
          .filter { filterByTestregel(it.loeysingId, testregelIds) }
          .map {
            TestresultatDetaljert(
                null,
                it.loeysingId,
                getTestregelIdFromSchema(it.testregelId).let { id -> id ?: 0 },
                it.testregelId,
                maalingId,
                it.side,
                it.suksesskriterium,
                it.testVartUtfoert,
                it.elementUtfall,
                it.elementResultat,
                it.elementOmtale,
                null,
                null,
                null)
          }
    }
    return emptyList()
  }

  fun getResultatForAutomatiskMaaling(
      maalingId: Int,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {

    val testresultat: List<AutotesterTestresultat>? =
        maalingResource.getTestresultat(maalingId, loeysingId)?.getOrThrow()

    if (!testresultat.isNullOrEmpty() && testresultat.first() is TestResultat) {
      return testresultat
          .map { it as TestResultat }
          .map {
            TestresultatDetaljert(
                null,
                it.loeysingId,
                getTestregelIdFromSchema(it.testregelId).let { id -> id ?: 0 },
                it.testregelId,
                maalingId,
                it.side,
                it.suksesskriterium,
                it.testVartUtfoert,
                it.elementUtfall,
                it.elementResultat,
                it.elementOmtale,
                null,
                null,
                null)
          }
    }
    return emptyList()
  }

  fun getResulatForManuellKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): List<TestresultatDetaljert> {
    val testgrunnlag = testgrunnlagDao.getTestgrunnlagForKontroll(kontrollId).opprinneligTest
    val testresultat = testResultatDAO.getManyResults(testgrunnlag.id).getOrThrow()

    val testregelIds: List<Int> = testregelDAO.getTestregelForKrav(kravId).map { it.id }

    val sideutvalIds = testresultat.map { it.sideutvalId }.distinct()
    val sideutvalIdUrlMap: Map<Int, URL> = sideutvalDAO.getSideutvalUrlMapKontroll(sideutvalIds)

    return testresultat
        .filter { filterByTestregel(it.testregelId, testregelIds) }
        .map {
          val testregel: Testregel = getTestregel(it.testregelId)
          val url = sideutvalIdUrlMap[it.sideutvalId]
          requireNotNull(url) { "Ugyldig testresultat url manglar" }

          // it.testregel er databaseId ikkje feltet testregelId i db
          TestresultatDetaljert(
              it.id,
              it.loeysingId,
              it.testregelId,
              testregel.testregelId,
              it.testgrunnlagId,
              url,
              getSuksesskriteriumFromTestregel(testregel.kravId),
              testVartUtfoertToLocalTime(it.testVartUtfoert),
              it.elementUtfall,
              it.elementResultat,
              TestresultatDetaljert.ElementOmtale(
                  htmlCode = null, pointer = null, description = it.elementOmtale),
              it.brukar,
              it.kommentar,
              bildeService.getBildeListForTestresultat(it.id).getOrNull())
        }
  }

  private fun filterByLoeysing(loeysingId: Int, loeysingIdFilter: Int?): Boolean {
    if (loeysingIdFilter != null) {
      return loeysingId == loeysingIdFilter
    }
    return true
  }

  fun filterByTestregel(testregelId: Int, testregelIds: List<Int>): Boolean {
    return testregelIds.contains(testregelId)
  }

  fun getTestregel(idTestregel: Int): Testregel {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel ?: throw RuntimeException("Fant ikkje testregel med id $idTestregel")
    }
  }

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
    return testVartUtfoert?.atZone(ZoneId.of("Europe/Oslo"))?.toLocalDateTime()
  }

  fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return kravregisterClient.getWcagKrav(kravId).getOrNull()?.suksesskriterium?.let { listOf(it) }
        ?: emptyList()
  }

  fun getResultatList(type: Kontroll.Kontrolltype?): List<Resultat> {
    return when (type) {
      Kontroll.Kontrolltype.InngaaendeKontroll -> getManuellKontrollResultat()
      Kontroll.Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat()
      else -> getKontrollResultat()
    }
  }

  private fun getAutomatiskKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getTestresultatMaaling()
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return resultatDAO.getResultat().groupBy { it.id }.map { (id, result) -> resultat(id, result) }
  }

  fun getKontrollResultat(id: Int): List<Resultat> {
    return resultatDAO
        .getResultatKontroll(id)
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun getManuellKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getTestresultatTestgrunnlag()
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun resultat(id: Int, result: List<ResultatLoeysing>): Resultat {
    val loeysingar = getLoeysingMap(result).getOrThrow()
    val statusLoeysingar = progresjonPrLoeysing(id, result.first().typeKontroll, loeysingar)
    val resultLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)

    return Resultat(
        id, result.first().namn, result.first().typeKontroll, result.first().dato, resultLoeysingar)
  }

  private fun resultatPrLoeysing(
      result: List<ResultatLoeysing>,
      loeysingar: LoysingList,
      statusLoeysingar: Map<Int, Int>,
  ): List<LoeysingResultat> {
    val resultLoeysingar =
        result
            .groupBy { it.loeysingId }
            .map { (loeysingId, resultLoeysing) ->
              LoeysingResultat(
                  loeysingId,
                  loeysingar.getNamn(loeysingId),
                  loeysingar.getVerksemdNamn(loeysingId),
                  resultLoeysing.map { it.score }.average(),
                  resultLoeysing.first().testType,
                  resultLoeysing.map { it.talElementSamsvar }.sum() +
                      resultLoeysing.map { it.talElementSamsvar }.sum(),
                  resultLoeysing.map { it.talElementSamsvar }.sum(),
                  resultLoeysing.map { it.talElementBrot }.sum(),
                  getTestar(resultLoeysing.first().id, resultLoeysing.first().typeKontroll),
                  statusLoeysingar[loeysingId] ?: 0)
            }
    return resultLoeysingar
  }

  fun progresjonPrLoeysing(
      kontrollId: Int,
      kontrolltype: Kontroll.Kontrolltype,
      loeysingar: LoysingList
  ): Map<Int, Int> {
    if (kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
      return loeysingar.loeysingar.keys.associateWith { 100 }
    }

    val testgrunnlagId = testgrunnlagDao.getTestgrunnlagForKontroll(kontrollId).opprinneligTest.id

    val resultatPrSak = testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()
    val progresjon =
        resultatPrSak
            .groupBy { it.loeysingId }
            .entries
            .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
            .associateBy({ it.first }, { it.second })

    return progresjon
  }

  private fun percentageFerdig(result: List<ResultatManuellKontroll>): Int =
      (result.map { it.status }.count { it == ResultatManuellKontroll.Status.Ferdig }.toDouble() /
              result.size)
          .times(100)
          .toInt()

  private fun getLoeysingMap(result: List<ResultatLoeysing>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(result.map { it.loeysingId }).mapCatching {
        loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  private fun getTestar(id: Int, kontrolltype: Kontroll.Kontrolltype): List<String> {
    if (kontrolltype == Kontroll.Kontrolltype.InngaaendeKontroll) {
      return testResultatDAO.getBrukarForTestgrunnlag(id)
    } else if (kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
      return maalingDAO.getBrukarForMaaling(id)
    }
    return listOf("testar")
  }

  fun getKontrollLoeysingResultat(
      kontrollId: Int,
      loeysingId: Int
  ): List<ResultatOversiktLoeysing>? {

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        ?.map { krav -> mapKrav(krav) }
        ?.groupBy { it.kravId }
        ?.map { (_, result) ->
          val loeysingar = getLoeysingMap(result).getOrThrow()
          ResultatOversiktLoeysing(
              result.first().loeysingId,
              loeysingar.getNamn(result.first().loeysingId),
              result.first().typeKontroll,
              result.first().namn,
              result.map { it.testar }.flatten().distinct(),
              result.map { it.score }.average(),
              result.first().kravId ?: 0,
              result.first().kravTittel ?: "",
              result.map { it.talElementBrot }.sum() + result.map { it.talElementSamsvar }.sum(),
              result.map { it.talElementBrot }.sum(),
              result.map { it.talElementSamsvar }.sum())
        }
  }

  fun mapKrav(result: ResultatLoeysing): ResultatLoeysing {
    val krav =
        testregelDAO.getTestregel(result.testregelId)?.kravId?.let {
          kravregisterClient.getWcagKrav(it).getOrNull()
        }
    return result.copy(
        kravId = krav?.id,
        kravTittel = krav?.tittel,
        testar = getTestar(result.id, result.typeKontroll))
  }

  fun getResultatListKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravid: Int
  ): List<TestresultatDetaljert> {
    val typeKontroll =
        kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first().kontrolltype
    return when (typeKontroll) {
      Kontroll.Kontrolltype.ForenklaKontroll ->
          getResultatForAutomatiskKontroll(kontrollId, loeysingId, kravid)
      Kontroll.Kontrolltype.InngaaendeKontroll ->
          getResulatForManuellKontroll(kontrollId, loeysingId, kravid)
    }
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontroll.Kontrolltype?,
      startDato: LocalDate?,
      sluttDato: LocalDate?
  ): List<ResultatTema> =
      resultatDAO.getResultatPrTema(kontrollId, kontrolltype, startDato, sluttDato)

  class LoysingList(val loeysingar: Map<Int, Loeysing.Expanded>) {
    fun getNamn(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      return loeysing?.namn ?: ""
    }

    fun getVerksemdNamn(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      if (loeysing?.verksemd == null) return ""
      return loeysing.verksemd.namn
    }
  }
}
