package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class ResultatService(
    private val resultatDAO: ResultatDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val eksternResultatDAO: EksternResultatDAO,
    private val automatiskResultatService: AutomatiskResultatService,
    private val kravregisterClient: KravregisterClient,
    private val testregelCache: TestregelCache,
    private val kontrollResultatServiceFactory: KontrollResultatServiceFactory
) {

  val logger = LoggerFactory.getLogger(ResultatService::class.java)

  private fun getKontrollResultatCommon(
      fetchResults: () -> List<ResultatLoeysingDTO>,
  ): List<Resultat> {
    return fetchResults()
        .groupBy { it.id }
        .map { (id, result) -> resultatgruppertPrKontroll(id, result) }
  }

  private fun getKontrollResultat(): List<Resultat> {
    return getKontrollResultatCommon { resultatDAO.getAllResultat() }
        .map { it.copy(loeysingar = limitResultatList(it.loeysingar)) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultat(kontrollId: Int): List<Resultat> {
    return getKontrollResultatCommon { resultatDAO.getResultatKontroll(kontrollId) }
  }

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return automatiskResultatService.getResultatForMaaling(maalingId, loeysingId)
  }

  fun getResultatList(type: Kontrolltype?): List<Resultat> {
    if (type != null) {
      return getKontrollResultatCommon { getResultatService(type).getAlleResultat() }
    }
    return getKontrollResultat()
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultatMedType(kontrollId: Int, kontrolltype: Kontrolltype): List<Resultat> {
    val resultat = resultatForKontrollType(kontrolltype, kontrollId)
    return resultat
  }

  private fun resultatForKontrollType(kontrolltype: Kontrolltype, kontrollId: Int): List<Resultat> {
    return getKontrollResultatCommon {
      getResultatService(kontrolltype).getKontrollResultat(kontrollId)
    }
  }

  private fun resultatgruppertPrKontroll(
      kontrollId: Int,
      result: List<ResultatLoeysingDTO>,
  ): Resultat {
    val resultatLoeysingar = loeysingResultatList(result)
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

  private fun loeysingResultatList(result: List<ResultatLoeysingDTO>): List<LoeysingResultat> {
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
      kontrolltype: Kontrolltype,
  ): List<LoeysingResultat> {
    val loeysingar = getLoeysingMap(mapResultatToLoeysingId(result)).getOrThrow()
    val statusLoeysingar = progresjonPrLoeysing(testgrunnlagId, kontrolltype, loeysingar)
    val resultatLoeysingar = resultatPrLoeysing(result, loeysingar, statusLoeysingar)
    return resultatLoeysingar
  }

  private fun mapResultatToLoeysingId(result: List<ResultatLoeysingDTO>) =
      result.map { it.loeysingId }

  private fun erKontrollPublisert(result: List<ResultatLoeysingDTO>) =
      eksternResultatDAO.erKontrollPublisert(result.first().id, getKontrolltype(result))

  private fun resultatPrLoeysing(
      result: List<ResultatLoeysingDTO>,
      loeysingar: LoysingList,
      statusLoeysingar: Map<Int, Int>,
  ): List<LoeysingResultat> {
    val testarar = getBrukararForTest(result.first().id)

    val resultLoeysingar =
        result
            .groupBy { it.loeysingId }
            .map { (loeysingId, resultLoeysing) ->
              LoeysingResultat(
                  loeysingId,
                  loeysingar.getNamn(loeysingId),
                  loeysingar.getVerksemdNamn(loeysingId),
                  loeysingar.getOrgnr(loeysingId),
                  calculateScore(resultLoeysing),
                  resultLoeysing.first().testType,
                  talTestaElementDTO(resultLoeysing),
                  calculateTalElementSamsvar(resultLoeysing),
                  calculateTalElementBrot(resultLoeysing),
                  testarar,
                  statusLoeysingar[loeysingId] ?: 0)
            }

    return resultLoeysingar
  }

  fun getBrukararForTest(kontrollId: Int): List<String> {
    return getResultService(kontrollId).getBrukararForTest(kontrollId)
  }

  private fun limitResultatList(resultLoeysingar: List<LoeysingResultat>): List<LoeysingResultat> {
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
      loeysingar: LoysingList,
  ): Map<Int, Int> {
    return getResultatService(kontrolltype).progresjonPrLoeysing(testgrunnlagId, loeysingar)
  }

  private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  fun getKontrollLoeysingResultat(
      kontrollId: Int,
      loeysingId: Int,
  ): List<ResultatOversiktLoeysing> {

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .toResultatOversiktLoeysing()
  }

  fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int,
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
    return handleIkkjeForekomstGeneric(
        resultat, resultat.talElementBrot, resultat.talElementSamsvar) {
          it.copy(score = null)
        }
  }

  private fun mapTestregel(result: ResultatLoeysingDTO): ResultatLoeysing {
    val testregel = testregelCache.getTestregelById(result.testregelId)

    return ResultatLoeysing(
        id = result.id,
        testgrunnlagId = result.testgrunnlagId,
        namn = result.namn,
        typeKontroll = result.typeKontroll,
        testType = result.testType,
        dato = result.dato,
        testar = result.testar,
        loeysingId = result.loeysingId,
        score = result.score,
        talElementSamsvar = result.talElementSamsvar,
        talElementBrot = result.talElementBrot,
        testregelId = result.testregelId,
        testregeltTittel = testregel.namn,
        kravId = testregel.krav.id,
        kravTittel = testregel.krav.tittel)
  }

  @Observed(name = "resultatservice.getresultatforkontrollloeysingtestregel")
  fun getTestresultatDetaljerPrTestregel(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      sortPaginationParams: SortPaginationParams,
  ): List<TestresultatDetaljert> {
    return getResultService(kontrollId)
        .getResultatForKontroll(kontrollId, loeysingId, testregelId, sortPaginationParams)
  }

  fun getResultatPrKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int,
      sortPaginationParams: SortPaginationParams,
  ): List<TestresultatDetaljert> {
    return getResultService(kontrollId)
        .getTestresulatDetaljertForKrav(kontrollId, loeysingId, kravId, sortPaginationParams)
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontrolltype?,
      loeysingId: Int?,
      startDato: LocalDate?,
      sluttDato: LocalDate?,
  ): List<ResultatTema> {
    require(kontrollId != null) { "kontrollId kan ikkje vere null" }
    require(loeysingId != null) { "loeysingId kan ikkje vere null" }

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .groupBy { it.testregelId }
        .map {
            calculateResultatTema(it) }
        .groupBy { it.temaNamn }
        .map {
            sumResulatTema(it)
        }
  }

    private fun sumResulatTema(entry: Map.Entry<String, List<ResultatTema>>): ResultatTema {
        val items = entry.value
        return ResultatTema(
            temaNamn = entry.key,
            score = items.mapNotNull { it.score }.average(),
            talTestaElement = items.sumOf { it.talTestaElement },
            talElementBrot = items.sumOf { it.talElementBrot },
            talElementSamsvar = items.sumOf { it.talElementSamsvar },
            talVarsel = 0,
            talElementIkkjeForekomst = 0
        )
    }

    private fun sumResulatKrav(entry: Map.Entry<Int, List<ResultatKrav>>): ResultatKrav {
        val items = entry.value
        return ResultatKrav(
            kravId = entry.key,
            suksesskriterium = items.first().suksesskriterium,
            score = items.mapNotNull { it.score }.average(),
            talTestaElement = items.sumOf { it.talTestaElement },
            talElementBrot = items.sumOf { it.talElementBrot },
            talElementSamsvar = items.sumOf { it.talElementSamsvar },
            talElementVarsel = 0,
            talElementIkkjeForekomst = 0
        )
    }

  private fun calculateResultatTema(
      entry: Map.Entry<Int, List<ResultatLoeysingDTO>>
  ): ResultatTema {
    val testregel = testregelCache.getTestregelById(entry.key)
      val (score, talElementBrot, talElementSamsvar) = calculateScoreAndElements(entry.value)
    return ResultatTema(
        temaNamn = testregel.tema?.tema ?: "Utan tema",
        score = score,
        talTestaElement = talElementBrot + talElementSamsvar,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talVarsel = 0,
        talElementIkkjeForekomst = 0)
  }


  private fun calculateResultatKrav(
      entry: Map.Entry<Int, List<ResultatLoeysingDTO>>
  ): ResultatKrav {
    val testregel = testregelCache.getTestregelById(entry.key)
      val (score, talElementBrot, talElementSamsvar) = calculateScoreAndElements(entry.value)
    return ResultatKrav(
        kravId = testregel.krav.id,
        suksesskriterium = testregel.krav.suksesskriterium,
        score = score,
        talTestaElement = talElementBrot + talElementSamsvar,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talElementVarsel = 0,
        talElementIkkjeForekomst = 0)
  }


    private fun calculateScoreAndElements(result: List<ResultatLoeysingDTO>): Triple<Double, Int, Int> {
        val score = result.filter { filterIkkjeForekomst(it) }.map { it.score }.average()
        val talElementBrot = result.sumOf { it.talElementBrot }
        val talElementSamsvar = result.sumOf { it.talElementSamsvar }
        return Triple(score, talElementBrot, talElementSamsvar)
    }

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      loeysingId: Int?,
      fraDato: LocalDate?,
      tilDato: LocalDate?,
  ): List<ResultatKrav> {
    require(kontrollId != null) { "kontrollId kan ikkje vere null" }
    require(loeysingId != null) { "loeysingId kan ikkje vere null" }

    return resultatDAO
        .getResultatKontrollLoeysing(kontrollId, loeysingId)
        .groupBy { it.testregelId }
        .map { calculateResultatKrav(it) }
        .groupBy { it.kravId }
        .map {
            sumResulatKrav(it)
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

    fun getOrgnr(loeysingId: Int): String {
      val loeysing = loeysingar[loeysingId]
      if (loeysing?.verksemd == null) return ""
      return loeysing.verksemd.organisasjonsnummer
    }
  }

  private fun List<ResultatLoeysingDTO>.toResultatOversiktLoeysing():
      List<ResultatOversiktLoeysing> {
    val loeysingar = getLoeysingar()
    return this.map { resultat -> mapTestregel(resultat) }
        .groupBy { it.testregelId }
        .map { (testregelId, result) ->
          handleIkkjeForekomst(mapResultatOversiktLoeysing(result, loeysingar, testregelId))
        }
  }

  private fun mapResultatOversiktLoeysing(
      result: List<ResultatLoeysing>,
      loeysingar: LoysingList,
      testregelId: Int,
  ) =
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
          testregelId,
          result.first().testregeltTittel,
          talTestaElement(result),
          result.sumOf { it.talElementBrot },
          result.sumOf { it.talElementSamsvar })

  fun getBrotForRapportLoeysing(
      kontrollId: Int,
      loeysingId: Int,
  ): List<TestresultatDetaljert> {
    return getResultService(kontrollId)
        .getResultatBrotForKontroll(kontrollId, loeysingId)
        .sortedBy { it.side.toString() }
  }

  private fun getResultService(kontrollId: Int): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollId)
  }

  private fun getResultatService(kontrollType: Kontrolltype): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollType)
  }

  private inline fun <reified T> handleIkkjeForekomstGeneric(
      item: T,
      talElementBrot: Int,
      talElementSamsvar: Int,
      copyWithNullScore: (T) -> T
  ): T {
    return if (talElementBrot == 0 && talElementSamsvar == 0) {
      copyWithNullScore(item)
    } else {
      item
    }
  }

  private fun List<ResultatLoeysingDTO>.getLoeysingar(): LoysingList {
    return this.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }
  }

  private fun talTestaElement(result: List<ResultatLoeysing>) =
      result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar }

  fun getTalBrotForKontrollLoeysingTestregel(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int
  ): Result<Int> {
    return getResultService(kontrollId)
        .getTalBrotForKontrollLoeysingTestregel(kontrollId, loeysingId, testregelId)
  }

  fun getTalBrotForKontrollLoeysingKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): Result<Int> {
    return getResultService(kontrollId)
        .getTalBrotForKontrollLoeysingKrav(kontrollId, loeysingId, kravId)
  }
}
