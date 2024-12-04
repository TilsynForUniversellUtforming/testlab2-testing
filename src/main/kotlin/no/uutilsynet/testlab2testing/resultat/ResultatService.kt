package no.uutilsynet.testlab2testing.resultat

import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.*
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.LoggerFactory
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
    val testgrunnlagDAO: TestgrunnlagDAO,
    val bildeService: BildeService,
    val eksternResultatDAO: EksternResultatDAO
) {

  private val logger = LoggerFactory.getLogger(ResultatService::class.java)

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
          .map { testresultatDetaljertMaaling(it, maalingId) }
    }
    return emptyList()
  }

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {

    val testresultat: List<AutotesterTestresultat> =
        maalingResource.getTestresultat(maalingId, loeysingId).getOrThrow()

    if (testresultat.isNotEmpty() && testresultat.first() is TestResultat) {
      return testresultat
          .map { it as TestResultat }
          .map { testresultatDetaljertMaaling(it, maalingId) }
    }
    return emptyList()
  }

  private fun testresultatDetaljertMaaling(it: TestResultat, maalingId: Int) =
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

  fun getResulatForManuellKontroll(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): List<TestresultatDetaljert> {
    val testgrunnlag = testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest
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
      Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat(null)
      Kontrolltype.InngaaendeKontroll,
      Kontrolltype.Tilsyn,
      Kontrolltype.Uttalesak,
      Kontrolltype.Statusmaaling -> getManuellKontrollResultat(null)
      else -> getKontrollResultat()
    }
  }

  fun getAutomatiskKontrollResultat(maalingId: Int?): List<Resultat> {
    val resultatMaaling =
        maalingId?.let { resultatDAO.getTestresultatMaaling(it) }
            ?: resultatDAO.getTestresultatMaaling()
    return resultatMaaling.groupBy { it.id }.map { (id, result) -> resultat(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getAllResultat()
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

  @Cacheable("resultatKontroll")
  fun getKontrollResultatMedType(kontrollId: Int, kontrolltype: Kontrolltype): List<Resultat> {
    val id = getIdTestgrunnlagOrMaaling(kontrolltype, kontrollId)
    val resultat = resultatForKontrollType(kontrolltype, id)
    return resultat
  }

  private fun resultatForKontrollType(kontrolltype: Kontrolltype, id: Int): List<Resultat> {
    val resultat =
        when (kontrolltype) {
          Kontrolltype.Statusmaaling,
          Kontrolltype.ForenklaKontroll -> getAutomatiskKontrollResultat(id)
          Kontrolltype.InngaaendeKontroll,
          Kontrolltype.Tilsyn,
          Kontrolltype.Uttalesak, -> getManuellKontrollResultat(id)
        }
    return resultat
  }

  private fun getIdTestgrunnlagOrMaaling(kontrolltype: Kontrolltype, kontrollId: Int): Int {
    val id: Int? =
        when (kontrolltype) {
          Kontrolltype.ForenklaKontroll -> maalingDAO.getMaalingIdFromKontrollId(kontrollId)
          Kontrolltype.InngaaendeKontroll,
          Kontrolltype.Tilsyn,
          Kontrolltype.Uttalesak,
          Kontrolltype.Statusmaaling ->
              testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId).opprinneligTest.id
        }
    return id
        ?: throw RuntimeException(
            "Fant ikkje testgrunnlagId eller maaling for kontrollId $kontrollId")
  }

  private fun getManuellKontrollResultat(testgrunnlagId: Int?): List<Resultat> {
    val resultatTestgrunnlag =
        testgrunnlagId?.let { resultatDAO.getTestresultatTestgrunnlag(it) }
            ?: resultatDAO.getTestresultatTestgrunnlag()
    return resultatTestgrunnlag.groupBy { it.id }.map { (id, result) -> resultat(id, result) }
  }

  private fun resultat(testgrunnlagId: Int, result: List<ResultatLoeysingDTO>): Resultat {
    val loeysingar = getLoeysingMap(result.map { it.loeysingId }).getOrThrow()
    val statusLoeysingar =
        progresjonPrLoeysing(testgrunnlagId, result.first().typeKontroll, loeysingar)
    val resultLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)
    val publisert =
        eksternResultatDAO.erKontrollPublisert(
            result.first().testgrunnlagId, result.first().typeKontroll)

    return Resultat(
        result.first().id,
        result.first().namn,
        result.first().typeKontroll,
        resultLoeysingar.first().testType,
        result.first().dato,
        publisert,
        resultLoeysingar)
  }

  private fun resultatPrLoeysing(
      result: List<ResultatLoeysingDTO>,
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

    return resultLoeysingar
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

  private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
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
  ): List<ResultatOversiktLoeysing> {

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .toResultatOversiktLoeysing()
  }

  fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int
  ): List<ResultatOversiktLoeysing> {
    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .filter { it.testType == TestgrunnlagType.OPPRINNELEG_TEST }
        .toResultatOversiktLoeysing()
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

  fun mapKrav(result: ResultatLoeysingDTO): ResultatLoeysing {
    val krav =
        testregelDAO.getTestregel(result.testregelId)?.kravId?.let {
          kravregisterClient.getWcagKrav(it)
        }

    return ResultatLoeysing(
        id = result.id,
        testgrunnlagId = result.testgrunnlagId,
        namn = result.namn,
        typeKontroll = result.typeKontroll,
        testType = result.testType,
        dato = result.dato,
        testar = getTestar(result.id, result.typeKontroll),
        loeysingId = result.loeysingId,
        score = result.score,
        talElementSamsvar = result.talElementSamsvar,
        talElementBrot = result.talElementBrot,
        testregelId = result.testregelId,
        kravId = krav?.id,
        kravTittel = krav?.tittel)
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

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontrolltype?,
      loeysingId: Int?,
      startDato: LocalDate?,
      sluttDato: LocalDate?
  ): List<ResultatTema> =
      resultatDAO.getResultatPrTema(kontrollId, kontrolltype, loeysingId, startDato, sluttDato)

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      loeysingId: Int?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatKrav> {
    return resultatDAO
        .getResultatPrKrav(kontrollId, kontrollType, loeysingId, fraDato, tilDato)
        .map { it.toResultatKrav() }
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

  private fun List<ResultatLoeysingDTO>.toResultatOversiktLoeysing() =
      this.map { krav -> mapKrav(krav) }
          .groupBy { it.kravId }
          .map { (kravId, result) ->
            val loeysingar = getLoeysingMap(result.map { it.loeysingId }).getOrThrow()
            handleIkkjeForekomst(
                ResultatOversiktLoeysing(
                    result.first().loeysingId,
                    loeysingar.getNamn(result.first().loeysingId),
                    result.first().typeKontroll,
                    result.first().namn,
                    result.map { it.testar }.flatten().distinct(),
                    result.map { it.score }.average(),
                    kravId ?: 0,
                    result.first().kravTittel ?: "",
                    result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar },
                    result.sumOf { it.talElementBrot },
                    result.sumOf { it.talElementSamsvar }))
          }
}
