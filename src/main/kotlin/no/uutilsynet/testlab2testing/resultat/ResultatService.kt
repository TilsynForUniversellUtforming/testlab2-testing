package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

private const val RESULATA_LIST_LIMIT = 5

@Component
class ResultatService(
    private val resultatDAO: ResultatDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val eksternResultatDAO: EksternResultatDAO,
    private val automatiskResultatService: AutomatiskResultatService,
    private val kontrollResultatServiceFactory: KontrollResultatServiceFactory,
    private val resultatCalculator: ResultatCalculator,
    private val resultatMapper: ResultatMapper,
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
                  resultatCalculator.scoreOrZero(resultLoeysing),
                  resultLoeysing.first().testType,
                  resultatCalculator.talTestaElementFraDto(resultLoeysing),
                  resultatCalculator.talElementSamsvar(resultLoeysing),
                  resultatCalculator.talElementBrot(resultLoeysing),
                  testarar,
                  statusLoeysingar[loeysingId] ?: 0)
            }

    return resultLoeysingar
  }

  fun getBrukararForTest(kontrollId: Int): List<String> {
    return getResultService(kontrollId).getBrukararForTest(kontrollId)
  }

  private fun limitResultatList(resultLoeysingar: List<LoeysingResultat>): List<LoeysingResultat> {
    return if (resultLoeysingar.size > RESULATA_LIST_LIMIT) {
      resultLoeysingar.subList(0, RESULATA_LIST_LIMIT)
    } else resultLoeysingar
  }

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
    val resultat = resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId)
    return resultatMapper.toResultatOversiktLoeysing(resultat, getLoeysingar(resultat))
  }

  fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int,
  ): List<ResultatOversiktLoeysing> {
    val resultat =
        resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId).filter {
          it.testType == TestgrunnlagType.OPPRINNELEG_TEST
        }
    return resultatMapper.toResultatOversiktLoeysing(resultat, getLoeysingar(resultat))
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

    return getResultat(kontrollId, loeysingId, kontrolltype, startDato, sluttDato)
        .groupBy { it.testregelId }
        .map(resultatMapper::toResultatTema)
        .groupBy { it.temaNamn }
        .map(resultatMapper::sumResultatTema)
  }

  fun getResultat(
      kontrollId: Int?,
      loeysingId: Int?,
      kontrollType: Kontrolltype?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatLoeysingDTO> {
    return when {
      kontrollId != null -> getResultatByBKontroll(kontrollId, loeysingId)
      fraDato != null && tilDato != null -> getResultatByDateRange(fraDato, tilDato, kontrollType)
      kontrollType != null -> resultatDAO.getResultatByKontrollType(kontrollType)
      loeysingId == null -> resultatDAO.getAllResultat()
      else ->
          throw IllegalArgumentException("loeysingId kan ikkje vere null dersom kontrollId er null")
    }
  }

  fun getResultatByBKontroll(kontrollId: Int, loeysingId: Int?): List<ResultatLoeysingDTO> {
    if (loeysingId != null) {
      return resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId)
    }
    return resultatDAO.getResultatKontroll(kontrollId)
  }

  fun getResultatByDateRange(
      fraDato: LocalDate,
      tilDato: LocalDate,
      kontrollType: Kontrolltype?
  ): List<ResultatLoeysingDTO> {
    if (kontrollType != null) {
      return resultatDAO.getResultatByKontrollTypeAndDateRange(kontrollType, fraDato, tilDato)
    }
    return resultatDAO.getResultatByDateRange(fraDato, tilDato)
  }

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      loeysingId: Int?,
      fraDato: LocalDate?,
      tilDato: LocalDate?,
  ): List<ResultatKrav> {

    return getResultat(kontrollId, loeysingId, kontrollType, fraDato, tilDato)
        .groupBy { it.testregelId }
        .map(resultatMapper::toResultatKrav)
        .groupBy { it.kravId }
        .map(resultatMapper::sumResultatKrav)
  }

  fun getBrotForRapportLoeysing(
      kontrollId: Int,
      loeysingId: Int,
  ): List<TestresultatDetaljert> {
    return getResultService(kontrollId)
        .getResultatBrotForKontroll(kontrollId, loeysingId)
        .sortedBy { it.side.toString() }
  }

  fun getResultService(kontrollId: Int): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollId)
  }

  private fun getResultatService(kontrollType: Kontrolltype): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollType)
  }

  private fun getLoeysingar(resultat: List<ResultatLoeysingDTO>): LoysingList {
    return resultat.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }
  }

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
