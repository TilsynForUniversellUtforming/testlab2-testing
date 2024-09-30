package no.uutilsynet.testlab2testing.resultat

import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.AutotesterTestresultat
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingResource
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.TestResultat
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.cache.annotation.Cacheable
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
    val testgrunnlagDao: TestgrunnlagDAO,
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
          .filter { filterTestregelNoekkel(it.testregelId, testregelIds) }
          .map {
            TestresultatDetaljert(
                null,
                it.loeysingId,
                getTestregelIdFromSchema(it.testregelId) ?: 0,
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
                emptyList())
          }
    }
    return emptyList()
  }

  fun getResultatForAutomatiskMaaling(
      maalingId: Int,
      loeysingId: Int?
  ): List<TestresultatDetaljert> {

    val testresultat: List<AutotesterTestresultat> =
        maalingResource.getTestresultat(maalingId, loeysingId).getOrThrow()

    if (testresultat.isNotEmpty() && testresultat.first() is TestResultat) {
      return testresultat
          .map { it as TestResultat }
          .map {
            TestresultatDetaljert(
                null,
                it.loeysingId,
                getTestregelIdFromSchema(it.testregelId) ?: 0,
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
                emptyList())
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
        .filter { it.elementResultat != null }
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
              bildeService.listBildeForTestresultat(it.id).getOrNull())
        }
  }

  fun filterByTestregel(testregelId: Int, testregelIds: List<Int>): Boolean {
    return testregelIds.contains(testregelId)
  }

  fun filterTestregelNoekkel(testregelNoekkel: String, testregelId: List<Int>): Boolean {
    return testregelId.contains(getTestregelIdFromSchema(testregelNoekkel) ?: 0)
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
    return testVartUtfoert?.atZone(ZONEID_OSLO)?.toLocalDateTime()
  }

  fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return listOf(kravregisterClient.getSuksesskriteriumFromKrav(kravId))
  }

  fun getResultatList(type: Kontrolltype?): List<Resultat> {
    return when (type) {
      Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat()
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.Statusmaaling -> getManuellKontrollResultat()
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
    return resultatDAO
        .getResultat()
        .groupBy { it.testgrunnlagId }
        .map { (id, result) -> resultat(id, result) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultat(kontrollId: Int): List<Resultat> {
    return resultatDAO
        .getResultatKontroll(kontrollId)
        .groupBy { it.testgrunnlagId }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun getManuellKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getTestresultatTestgrunnlag()
        .groupBy { it.id }
        .map { (id, result) -> resultat(id, result) }
  }

  private fun resultat(testgrunnlagId: Int, result: List<ResultatLoeysing>): Resultat {
    val loeysingar = getLoeysingMap(result).getOrThrow()
    val statusLoeysingar =
        progresjonPrLoeysing(testgrunnlagId, result.first().typeKontroll, loeysingar)
    val resultLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)

    return Resultat(
        result.first().id,
        result.first().namn,
        result.first().typeKontroll,
        resultLoeysingar.first().testType,
        result.first().dato,
        resultLoeysingar)
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
                  resultLoeysing
                      .filter { !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar) }
                      .map { it.score }
                      .average(),
                  resultLoeysing.first().testType,
                  resultLoeysing.sumOf { it.talElementSamsvar } +
                      resultLoeysing.sumOf { it.talElementSamsvar },
                  resultLoeysing.sumOf { it.talElementSamsvar },
                  resultLoeysing.sumOf { it.talElementBrot },
                  getTestar(resultLoeysing.first().id, resultLoeysing.first().typeKontroll),
                  statusLoeysingar[loeysingId] ?: 0)
            }

    return if (resultLoeysingar.size > 5) {
      resultLoeysingar.subList(0, 5)
    } else resultLoeysingar
  }

  fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      kontrolltype: Kontrolltype,
      loeysingar: LoysingList
  ): Map<Int, Int> {
    if (kontrolltype == Kontrolltype.ForenklaKontroll) {
      return loeysingar.loeysingar.keys.associateWith { 100 }
    }

    val resultatPrTestgrunnlag = testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()
    val progresjon =
        resultatPrTestgrunnlag
            .groupBy { it.loeysingId }
            .entries
            .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
            .associateBy({ it.first }, { it.second })

    return progresjon
  }

  private fun percentageFerdig(result: List<ResultatManuellKontroll>): Int =
      (result
              .map { it.status }
              .count { it == ResultatManuellKontrollBase.Status.Ferdig }
              .toDouble() / result.size)
          .times(100)
          .toInt()

  private fun getLoeysingMap(result: List<ResultatLoeysing>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(result.map { it.loeysingId }).mapCatching {
        loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  private fun getTestar(id: Int, kontrolltype: Kontrolltype): List<String> {
    return when (kontrolltype) {
      Kontrolltype.ForenklaKontroll -> maalingDAO.getBrukarForMaaling(id)
      else -> testResultatDAO.getBrukarForTestgrunnlag(id)
    }
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
          handleIkkjeForekomst(
              ResultatOversiktLoeysing(
                  result.first().loeysingId,
                  loeysingar.getNamn(result.first().loeysingId),
                  result.first().typeKontroll,
                  result.first().namn,
                  result.map { it.testar }.flatten().distinct(),
                  result
                      .filter { !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar) }
                      .map { it.score }
                      .average(),
                  result.first().kravId ?: 0,
                  result.first().kravTittel ?: "",
                  result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar },
                  result.sumOf { it.talElementBrot },
                  result.sumOf { it.talElementSamsvar }))
        }
  }

  fun erIkkjeForekomst(talElementBrot: Int, talElementSamsvar: Int): Boolean {
    return talElementBrot == 0 && talElementSamsvar == 0
  }

  fun handleIkkjeForekomst(resultat: ResultatOversiktLoeysing): ResultatOversiktLoeysing {
    if (erIkkjeForekomst(resultat.talElementBrot, resultat.talElementSamsvar)) {
      return resultat.copy(score = null)
    }
    return resultat
  }

  fun mapKrav(result: ResultatLoeysing): ResultatLoeysing {
    val krav =
        testregelDAO.getTestregel(result.testregelId)?.kravId?.let {
          kravregisterClient.getWcagKrav(it)
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
      Kontrolltype.ForenklaKontroll ->
          getResultatForAutomatiskKontroll(kontrollId, loeysingId, kravid)
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.Statusmaaling -> getResulatForManuellKontroll(kontrollId, loeysingId, kravid)
    }
  }

  fun getResultatListTestgrunnlag(
      testgrunnlagId: Int,
      loeysingId: Int,
      kravid: Int
  ): List<TestresultatDetaljert> {
    val kontrollId = testgrunnlagDao.getTestgrunnlag(testgrunnlagId).getOrThrow().kontrollId

    val typeKontroll =
        kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first().kontrolltype
    return when (typeKontroll) {
      Kontroll.Kontrolltype.ForenklaKontroll ->
          getResultatForAutomatiskKontroll(kontrollId, loeysingId, kravid)
      Kontroll.Kontrolltype.InngaaendeKontroll,
      Kontroll.Kontrolltype.Tilsyn,
      Kontroll.Kontrolltype.Uttalesak,
      Kontroll.Kontrolltype.Statusmaaling ->
          getResulatForManuellKontroll(kontrollId, loeysingId, kravid)
    }
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontrolltype?,
      startDato: LocalDate?,
      sluttDato: LocalDate?
  ): List<ResultatTema> =
      resultatDAO.getResultatPrTema(kontrollId, kontrolltype, startDato, sluttDato)

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatKrav> {
    return resultatDAO.getResultatPrKrav(kontrollId, kontrollType, fraDato, tilDato).map {
      it.toResultatKrav()
    }
  }

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

  fun ResultatKravBase.toResultatKrav(): ResultatKrav {
    return ResultatKrav(
        suksesskriterium = kravregisterClient.getSuksesskriteriumFromKrav(kravId),
        score = score,
        talTestaElement =
            talElementBrot + talElementSamsvar + talElementVarsel + talElementIkkjeForekomst,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talElementVarsel = talElementVarsel,
        talElementIkkjeForekomst = talElementIkkjeForekomst)
  }

  private fun List<ResultatLoeysing>.toResultatOversiktLoeysing() =
      this.map { krav -> mapKrav(krav) }
          .groupBy { it.kravId }
          .map { (_, result) ->
            val loeysingar = getLoeysingMap(result).getOrThrow()
            handleIkkjeForekomst(
                ResultatOversiktLoeysing(
                    result.first().loeysingId,
                    loeysingar.getNamn(result.first().loeysingId),
                    result.first().typeKontroll,
                    result.first().namn,
                    result.map { it.testar }.flatten().distinct(),
                    result.map { it.score }.average(),
                    result.first().kravId ?: 0,
                    result.first().kravTittel ?: "",
                    result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar },
                    result.sumOf { it.talElementBrot },
                    result.sumOf { it.talElementSamsvar }))
          }
}
