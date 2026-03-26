package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestgrunnlagType
import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import no.uutilsynet.testlab2testing.ekstern.resultat.EksternResultatDAO
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.resultat.common.LoysingList
import no.uutilsynet.testlab2testing.resultat.common.ResultatMapper
import no.uutilsynet.testlab2testing.resultat.repository.ResultatDAO
import no.uutilsynet.testlab2testing.resultat.service.AutomatiskResultatService
import no.uutilsynet.testlab2testing.resultat.service.KontrollResultatService
import no.uutilsynet.testlab2testing.resultat.service.KontrollResultatServiceFactory
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ResultatService(
    private val resultatDAO: ResultatDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val eksternResultatDAO: EksternResultatDAO,
    private val automatiskResultatService: AutomatiskResultatService,
    private val testregelCache: TestregelCache,
    private val kontrollResultatServiceFactory: KontrollResultatServiceFactory
) {

  val logger = LoggerFactory.getLogger(ResultatService::class.java)

  private fun getKontrollResultatCommon(
      fetchResults: () -> List<ResultatPerTestregelDTO>,
  ): List<Resultat> {
    return ResultatAggregator.getKontrollResultatCommon(fetchResults, ::resultatgruppertPrKontroll)
  }

  private fun getKontrollResultat(): List<Resultat> {
    return getKontrollResultatCommon { resultatDAO.getAllResultat() }
        .map { it.copy(loeysingar = limitResultatList(it.loeysingar)) }
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultat(kontrollId: Int): List<Resultat> {
    return getKontrollResultatCommon { getResultsPerTestregel(kontrollId,null) }
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
      return getKontrollResultatCommon {
          getResultatService(kontrolltype).getKontrollResultat(kontrollId)
      }
  }

    private fun resultatgruppertPrKontroll(
      kontrollId: Int,
      result: List<ResultatPerTestregelDTO>,
  ): Resultat {
    return Resultat(
        kontrollId,
        result.first().namn,
        getKontrolltype(result),
        result.first().testType,
        result.first().dato,
        erKontrollPublisert(result),
        loeysingResultatList(result, kontrollId))
  }

  private fun loeysingResultatList(result: List<ResultatPerTestregelDTO>, kontrollId: Int): List<LoeysingResultat> {
        return result
            .groupBy { it.testgrunnlagId }
            .map { (_, result) ->
                resultatPrLoeysing(result,kontrollId)
            }
            .flatten()
  }


    private fun erKontrollPublisert(result: List<ResultatPerTestregelDTO>) =
      eksternResultatDAO.erKontrollPublisert(result.first().id, getKontrolltype(result))

    private fun resultatPrLoeysing(
        result: List<ResultatPerTestregelDTO>,
        kontrollId: Int,
    ): List<LoeysingResultat> {
        val first  = result.first()
        val testarar = getBrukararForTest(first.id)
        val loeysingar = getLoeysingMap(result.map { it.loeysingId }).getOrThrow()
        val statusLoeysingar =
            progresjonPrLoeysing(first, loeysingar,kontrollId)


        val resultLoeysingar =
            result
                .groupBy { it.loeysingId }
                .map { (loeysingId, resultLoeysing) ->
                    LoeysingResultat(
                        loeysingId,
                        loeysingar.getNamn(loeysingId),
                        loeysingar.getVerksemdNamn(loeysingId),
                        loeysingar.getOrgnr(loeysingId),
                        ResultatMapper.calculateScore(resultLoeysing),
                        first.testType,
                        ResultatMapper.talTestaElementDTO(resultLoeysing),
                        ResultatMapper.calculateTalElementSamsvar(resultLoeysing),
                        ResultatMapper.calculateTalElementBrot(resultLoeysing),
                        testarar,
                        statusLoeysingar[loeysingId] ?: 0
                    )
                }

        return resultLoeysingar
    }

  fun getBrukararForTest(kontrollId: Int): List<String> {
    return getResultatService(kontrollId).getBrukararForTest(kontrollId)
  }

  private fun limitResultatList(resultLoeysingar: List<LoeysingResultat>): List<LoeysingResultat> {
    return if (resultLoeysingar.size > 5) {
      resultLoeysingar.subList(0, 5)
    } else resultLoeysingar
  }

    fun getKontrollLoeysingResultat(
        kontrollId: Int,
        loeysingId: Int?,
    ): List<ResultatOversiktLoeysing> {
        return getResultsPerTestregel(kontrollId, loeysingId)
            .toResultatOversiktLoeysing()
    }

    private fun getResultsPerTestregel(
        kontrollId: Int,
        loeysingId: Int?
    ): List<ResultatPerTestregelDTO> {
        return kontrollResultatServiceFactory.getResultatService(kontrollId).getKontrollResultat(kontrollId)
            .filter { it.loeysingId == loeysingId || loeysingId == null }
    }



    private fun getAggregatedData(resultatMeta: ResultatMetadata): List<AggregeringPerTestregelEntity> {
        return kontrollResultatServiceFactory
            .getAggregatedResultatService(resultatMeta.kontrollId)
            .getAggregatedDataPerTestregel(resultatMeta)
    }

    fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int,
  ): List<ResultatOversiktLoeysing> {
    return getResultsPerTestregel(kontrollId, loeysingId)
        .filter { it.testType == TestgrunnlagType.OPPRINNELEG_TEST }
        .toResultatOversiktLoeysing()
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

        return getResultsPerTestregel(kontrollId, loeysingId)
            .groupBy { it.testregelId }
            .map { calculateResultatKrav(it) }
            .groupBy { it.kravId }
            .map { sumResultatKrav(it) }
    }

  private fun sumResultatKrav(entry: Map.Entry<Int, List<ResultatKrav>>): ResultatKrav {
    val items = entry.value
    return ResultatKrav(
        kravId = entry.key,
        suksesskriterium = items.first().suksesskriterium,
        score = items.mapNotNull { it.score }.average(),
        talTestaElement = items.sumOf { it.talTestaElement },
        talElementBrot = items.sumOf { it.talElementBrot },
        talElementSamsvar = items.sumOf { it.talElementSamsvar },
        talElementVarsel = 0,
        talElementIkkjeForekomst = 0)
  }

  private fun calculateResultatKrav(
      entry: Map.Entry<Int, List<ResultatPerTestregelDTO>>
  ): ResultatKrav {
    val testregel = testregelCache.getTestregelById(entry.key)
    val (score, talElementBrot, talElementSamsvar) = ResultatMapper.calculateScoreAndElements(entry.value)
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

  fun getBrotForRapportLoeysing(
      kontrollId: Int,
      loeysingId: Int,
  ): List<TestresultatDetaljert> {
    return getResultatService(kontrollId)
        .getResultatBrotForKontroll(kontrollId, loeysingId)
        .sortedBy { it.side.toString() }
  }

  fun getResultatService(kontrollId: Int): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollId)
  }

  private fun getResultatService(kontrollType: Kontrolltype): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollType)
  }

  // Restore required helper functions for unresolved references
  private fun getKontrolltype(result: List<ResultatPerTestregelDTO>) = result.first().typeKontroll

  private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  private fun progresjonPrLoeysing(
      resultatData: ResultatPerTestregelDTO,
      loeysingar: LoysingList,
      kontrollId: Int,
  ): Map<Int, Int> {
    return getResultatService(kontrollId).progresjonPrLoeysing(resultatData, loeysingar)
  }

  fun getTestresultatDetaljerPrTestregel(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDetaljert> {
    return getResultatService(kontrollId)
        .getResultatForKontroll(kontrollId, loeysingId, testregelId, sortPaginationParams)
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      loeysingId: Int?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatTema> {
    require(kontrollId != null) { "kontrollId kan ikkje vere null" }
    require(loeysingId != null) { "loeysingId kan ikkje vere null" }


    return getResultsPerTestregel(kontrollId, loeysingId)
        .groupBy { it.testregelId }
        .map { entry ->
          val testregel = testregelCache.getTestregelById(entry.key)
          val (score, talElementBrot, talElementSamsvar) = ResultatMapper.calculateScoreAndElements(entry.value)
          ResultatTema(
              temaNamn = testregel.tema?.tema ?: "Utan tema",
              score = score,
              talTestaElement = talElementBrot + talElementSamsvar,
              talElementBrot = talElementBrot,
              talElementSamsvar = talElementSamsvar,
              talVarsel = 0,
              talElementIkkjeForekomst = 0)
        }
        .groupBy { it.temaNamn }
        .map { entry ->
          val items = entry.value
          ResultatTema(
              temaNamn = entry.key,
              score = items.mapNotNull { it.score }.average(),
              talTestaElement = items.sumOf { it.talTestaElement },
              talElementBrot = items.sumOf { it.talElementBrot },
              talElementSamsvar = items.sumOf { it.talElementSamsvar },
              talVarsel = 0,
              talElementIkkjeForekomst = 0)
        }
  }

  // Extension function for unresolved reference
  private fun List<ResultatPerTestregelDTO>.toResultatOversiktLoeysing(): List<ResultatOversiktLoeysing> {
    val loeysingar = this.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }

    return this.map { ResultatMapper.mapTestregel(it, testregelCache) }
        .groupBy { it.testregelId }
        .map { (testregelId, result) ->
          ResultatMapper.handleIkkjeForekomst(
            ResultatMapper.mapResultatOversiktLoeysing(result, loeysingar, testregelId)
          )
        }
  }

    fun getResultatPrKrav(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int,
        sortPaginationParams: SortPaginationParams,
    ): List<TestresultatDetaljert> {
        return getResultatService(kontrollId)
            .getTestresulatDetaljertForKrav(kontrollId, loeysingId, kravId, sortPaginationParams)
    }

    fun getTalBrotForKontrollLoeysingKrav(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int
    ): Result<Int> {
        return getResultatService(kontrollId)
            .getTalBrotForKontrollLoeysingKrav(kontrollId, loeysingId, kravId)
    }

    fun getTalBrotForKontrollLoeysingTestregel(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int
    ): Result<Int> {
        return getResultatService(kontrollId)
            .getTalBrotForKontrollLoeysingTestregel(kontrollId, loeysingId, testregelId)
    }

}
