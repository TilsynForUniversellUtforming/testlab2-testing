package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.Constants.Companion.ZONEID_OSLO
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingResource
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.TestResultat
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
    val maalingId = getMaalingForKontroll(kontrollId)
    val testregelIds: List<Int> = getTestreglarForKrav(kravId)

    val testresultat: List<TestResultat> = getAutotesterTestresultat(maalingId, loeysingId)

    if (testresultat.isNotEmpty()) {
      return testresultat
          .filter { filterTestregelNoekkel(it.testregelId, testregelIds) }
          .map { testresultatDetaljertMaaling(it, maalingId) }
    }
    return emptyList()
  }

  private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
    val testresultat: List<TestResultat> =
        maalingResource.getTestresultat(maalingId, loeysingId).getOrThrow().map {
          it as TestResultat
        }
    return testresultat
  }

  private fun getMaalingForKontroll(kontrollId: Int): Int {
    val maalingId =
        maalingDAO.getMaalingIdFromKontrollId(kontrollId)
            ?: throw RuntimeException("Fant ikkje maalingId for kontrollId $kontrollId")
    return maalingId
  }

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return getAutotesterTestresultat(maalingId, loeysingId).map {
      testresultatDetaljertMaaling(it, maalingId)
    }
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
    val testresultat = getTestresultatForKontroll(kontrollId, loeysingId)
    val testregelIdsForKrav: List<Int> = getTestreglarForKrav(kravId)

    val sideutvalIdUrlMap: Map<Int, URL> = getSideutvalMap(testresultat)

    return testresultat
        .filter { filterByTestregel(it.testregelId, testregelIdsForKrav) }
        .filter { it.elementResultat != null }
        .map { resultatManuellKontrollTotestresultatDetaljert(it, sideutvalIdUrlMap) }
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

  private fun resultatManuellKontrollTotestresultatDetaljert(
      it: ResultatManuellKontroll,
      sideutvalIdUrlMap: Map<Int, URL>
  ): TestresultatDetaljert {
    val testregel: Testregel = getTestregel(it.testregelId)
    // it.testregel er databaseId ikkje feltet testregelId i db
    return TestresultatDetaljert(
        it.id,
        it.loeysingId,
        it.testregelId,
        testregel.testregelId,
        it.testgrunnlagId,
        getUrl(sideutvalIdUrlMap, it),
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

  private fun getUrl(sideutvalIdUrlMap: Map<Int, URL>, it: ResultatManuellKontroll): URL {
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

  private fun getTestreglarForKrav(kravId: Int): List<Int> {
    val testregelIds: List<Int> = testregelDAO.getTestregelForKrav(kravId).map { it.id }
    return testregelIds
  }

  private fun filterByTestregel(testregelId: Int, testregelIds: List<Int>): Boolean {
    return testregelIds.contains(testregelId)
  }

  private fun filterTestregelNoekkel(testregelNoekkel: String, testregelId: List<Int>): Boolean {
    return testregelId.contains(getTestregelIdFromSchema(testregelNoekkel) ?: 0)
  }

  private fun getTestregel(idTestregel: Int): Testregel {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel ?: throw RuntimeException("Fant ikkje testregel med id $idTestregel")
    }
  }

  private fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  private fun testVartUtfoertToLocalTime(testVartUtfoert: Instant?): LocalDateTime? {
    return testVartUtfoert?.atZone(ZONEID_OSLO)?.toLocalDateTime()
  }

  private fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
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
    return resultatMaaling
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return resultatDAO
        .getAllResultat()
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultat(kontrollId: Int): List<Resultat> {
    return resultatDAO
        .getResultatKontroll(kontrollId)
        .groupBy { it.testgrunnlagId }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
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
    return resultatTestgrunnlag
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun resultatgruppertPrKontroll(
      kontrollId: Int,
      result: List<ResultatLoeysingDTO>
  ): Resultat {

    val resultatLoeysingar = loeysingResultats(result)

    val publisert = erKontrollPublisert(result)

    return Resultat(
        kontrollId,
        result.first().namn,
        getKontrolltype(result),
        resultatLoeysingar.first().testType,
        result.first().dato,
        publisert,
        resultatLoeysingar)
  }

  private fun loeysingResultats(result: List<ResultatLoeysingDTO>): List<LoeysingResultat> {
    val resultatLoeysingar =
        result
            .groupBy { it.testgrunnlagId }
            .map { (id, result) ->
              resultatForLoeysingarPrTestgrunnlag(result, id, getKontrolltype(result))
            }
            .flatten()
    return resultatLoeysingar
  }

  private fun resultatForLoeysingarPrTestgrunnlag(
      result: List<ResultatLoeysingDTO>,
      testgrunnlagId: Int,
      kontrolltype: Kontrolltype
  ): List<LoeysingResultat> {
    val loeysingar = getLoeysingMap(mapResultatToLoeysingId(result)).getOrThrow()
    val statusLoeysingar = progresjonPrLoeysing(testgrunnlagId, kontrolltype, loeysingar)
    val resultatLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)
    return resultatLoeysingar
  }

  private fun mapResultatToLoeysingId(result: List<ResultatLoeysingDTO>) =
      result.map { it.loeysingId }

  private fun erKontrollPublisert(result: List<ResultatLoeysingDTO>) =
      eksternResultatDAO.erKontrollPublisert(result.first().testgrunnlagId, getKontrolltype(result))

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
                  calculateScore(resultLoeysing),
                  resultLoeysing.first().testType,
                  talTestaElementDTO(resultLoeysing),
                  calculateTalElementSamsvar(resultLoeysing),
                  calculateTalElementBrot(resultLoeysing),
                  getTestar(resultLoeysing.first().id, getKontrolltype(resultLoeysing)),
                  statusLoeysingar[loeysingId] ?: 0)
            }

    return if (resultLoeysingar.size > 5) {
      resultLoeysingar.subList(0, 5)
    } else resultLoeysingar
  }

  private fun calculateTalElementBrot(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.sumOf { it.talElementBrot }

  private fun talTestaElementDTO(resultLoeysing: List<ResultatLoeysingDTO>) =
      calculateTalElementSamsvar(resultLoeysing) + calculateTalElementSamsvar(resultLoeysing)

  private fun calculateTalElementSamsvar(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.sumOf { it.talElementSamsvar }

  private fun calculateScore(resultLoeysing: List<ResultatLoeysingDTO>) =
      resultLoeysing.filter { filterIkkjeForekomst(it) }.map { it.score }.average()

  private fun filterIkkjeForekomst(it: ResultatLoeysingDTO) =
      !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar)

  private fun getKontrolltype(result: List<ResultatLoeysingDTO>) = result.first().typeKontroll

  private fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      kontrolltype: Kontrolltype,
      loeysingar: LoysingList
  ): Map<Int, Int> {
    if (kontrolltype == Kontrolltype.ForenklaKontroll) {
      return loeysingar.loeysingar.keys.associateWith { 100 }
    }

    val resultatPrTestgrunnlag = getResultatPrTestgrunnlag(testgrunnlagId)
    val progresjon =
        resultatPrTestgrunnlag
            .groupBy { it.loeysingId }
            .entries
            .map { (loeysingId, result) -> Pair(loeysingId, percentageFerdig(result)) }
            .associateBy({ it.first }, { it.second })

    return progresjon
  }

  private fun getResultatPrTestgrunnlag(testgrunnlagId: Int) =
      testResultatDAO.getManyResults(testgrunnlagId).getOrThrow()

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

  private fun getTestar(brukarId: Int, kontrolltype: Kontrolltype): List<String> {
    return when (kontrolltype) {
      Kontrolltype.ForenklaKontroll -> maalingDAO.getBrukarForMaaling(brukarId)
      else -> testResultatDAO.getBrukarForTestgrunnlag(brukarId)
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

  private fun erIkkjeForekomst(talElementBrot: Int, talElementSamsvar: Int): Boolean {
    return talElementBrot == 0 && talElementSamsvar == 0
  }

  private fun handleIkkjeForekomst(resultat: ResultatOversiktLoeysing): ResultatOversiktLoeysing {
    if (erIkkjeForekomst(resultat.talElementBrot, resultat.talElementSamsvar)) {
      return resultat.copy(score = null)
    }
    return resultat
  }

  private fun mapKrav(result: ResultatLoeysingDTO): ResultatLoeysing {
    val krav = getKravWcag2x(result)

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

  private fun getKravWcag2x(result: ResultatLoeysingDTO): KravWcag2x? {
    val krav = getTestregel(result)?.kravId?.let { kravregisterClient.getWcagKrav(it) }
    return krav
  }

  private fun getTestregel(result: ResultatLoeysingDTO) =
      testregelDAO.getTestregel(result.testregelId)

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

  private fun List<ResultatLoeysingDTO>.toResultatOversiktLoeysing():
      List<ResultatOversiktLoeysing> {

    val loeysingar = this.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }

    return this.map { krav -> mapKrav(krav) }
        .groupBy { it.kravId }
        .map { (kravId, result) ->
          ResultatOversiktLoeysing(
                  result.first().loeysingId,
                  loeysingar.getNamn(result.first().loeysingId),
                  result.first().typeKontroll,
                  result.first().namn,
                  result.map { it.testar }.flatten().distinct(),
                  result.map { it.score }.average(),
                  kravId ?: 0,
                  result.first().kravTittel ?: "",
                  talTestaElement(result),
                  result.sumOf { it.talElementBrot },
                  result.sumOf { it.talElementSamsvar })
              .let { handleIkkjeForekomst(it) }
        }
  }

  private fun talTestaElement(result: List<ResultatLoeysing>) =
      result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar }
}
