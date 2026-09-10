package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.resultMappers.ResultKontrollMapper
import no.uutilsynet.testlab2testing.resultat.resultMappers.ResultatLoeysingMapper
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

private const val RESULATA_LIST_LIMIT = 5

@Suppress("LongParameterList")
@Component
class ResultatService(
    private val resultatDAO: ResultatDAO,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val automatiskResultatService: AutomatiskResultatService,
    private val kontrollResultatServiceFactory: KontrollResultatServiceFactory,
    private val resultatLoeysingMapper: ResultatLoeysingMapper,
    private val resultatAggregator: ResultKontrollMapper
) {

  val logger: Logger = LoggerFactory.getLogger(ResultatService::class.java)

  @Cacheable("resultatKontroll")
  fun getKontrollResultatByKontrollId(kontrollId: Int): List<Resultat> {
    return resultatAggregator.getKontrollResultatCommon { resultatDAO.getResultatKontroll(kontrollId) }
  }

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return automatiskResultatService.getResultatForMaaling(maalingId, loeysingId)
  }

  fun getResultatList(type: Kontrolltype?): List<Resultat> {
    if (type != null) {
      return resultatAggregator.getKontrollResultatCommon { getResultatService(type).getAlleResultat() }
    }
    return getKontrollResultat()
  }

  @Cacheable("resultatKontroll")
  fun getKontrollResultatMedType(kontrollId: Int, kontrolltype: Kontrolltype): List<Resultat> {
    return resultatForKontrollType(kontrolltype, kontrollId)
  }


  fun getKontrollLoeysingResultat(
      kontrollId: Int,
      loeysingId: Int,
  ): List<ResultatOversiktLoeysing> {
    val resultat = resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId)
    return resultatLoeysingMapper.toResultatOversiktLoeysing(resultat, getLoeysingar(resultat))
  }

  fun getKontrollLoeysingResultatIkkjeRetest(
      kontrollId: Int,
      loeysingId: Int,
  ): List<ResultatOversiktLoeysing> {
    val resultat =
        resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId).filter {
          it.testType == TestgrunnlagType.OPPRINNELEG_TEST
        }
    return resultatLoeysingMapper.toResultatOversiktLoeysing(resultat, getLoeysingar(resultat))
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
        .map(resultatLoeysingMapper::toResultatTema)
        .groupBy { it.temaNamn }
        .map(resultatLoeysingMapper::sumResultatTema)
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

    fun getResultatPrKrav(
        kontrollId: Int?,
        kontrollType: Kontrolltype?,
        loeysingId: Int?,
        fraDato: LocalDate?,
        tilDato: LocalDate?,
    ): List<ResultatKrav> {

        return getResultat(kontrollId, loeysingId, kontrollType, fraDato, tilDato)
            .groupBy { it.testregelId }
            .map(resultatLoeysingMapper::toResultatKrav)
            .groupBy { it.kravId }
            .map(resultatLoeysingMapper::sumResultatKrav)
    }

    fun getBrotForRapportLoeysing(
        kontrollId: Int,
        loeysingId: Int,
    ): List<TestresultatDetaljert> {
        return getResultService(kontrollId)
            .getResultatBrotForKontroll(kontrollId, loeysingId)
            .sortedBy { it.side.toString() }
    }

  private fun getResultat(
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

  private fun getResultatByBKontroll(kontrollId: Int, loeysingId: Int?): List<ResultatLoeysingDTO> {
    if (loeysingId != null) {
      return resultatDAO.getResultatKontrollLoeysing(kontrollId, loeysingId)
    }
    return resultatDAO.getResultatKontroll(kontrollId)
  }

  private fun getResultatByDateRange(
      fraDato: LocalDate,
      tilDato: LocalDate,
      kontrollType: Kontrolltype?
  ): List<ResultatLoeysingDTO> {
    if (kontrollType != null) {
      return resultatDAO.getResultatByKontrollTypeAndDateRange(kontrollType, fraDato, tilDato)
    }
    return resultatDAO.getResultatByDateRange(fraDato, tilDato)
  }



  private fun getResultService(kontrollId: Int): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollId)
  }


    private fun getKontrollResultat(): List<Resultat> {
    return resultatAggregator.getKontrollResultatCommon { resultatDAO.getAllResultat() }
        .map { it.copy(loeysingar = limitResultatList(it.loeysingar)) }
  }

  private fun resultatForKontrollType(kontrolltype: Kontrolltype, kontrollId: Int): List<Resultat> {
    return resultatAggregator.getKontrollResultatCommon {
      getResultatService(kontrolltype).getKontrollResultat(kontrollId)
    }
  }

    private fun limitResultatList(resultLoeysingar: List<LoeysingResultat>): List<LoeysingResultat> {
    return if (resultLoeysingar.size > RESULATA_LIST_LIMIT) {
      resultLoeysingar.subList(0, RESULATA_LIST_LIMIT)
    } else resultLoeysingar
  }

    private fun getLoeysingMap(listLoysingId: List<Int>): Result<LoysingList> {
    return loeysingsRegisterClient.getManyExpanded(listLoysingId).mapCatching { loeysingList ->
      LoysingList(loeysingList.associateBy { it.id })
    }
  }

  private fun getResultatService(kontrollType: Kontrolltype): KontrollResultatService {
    return kontrollResultatServiceFactory.getResultatService(kontrollType)
  }

  private fun getLoeysingar(resultat: List<ResultatLoeysingDTO>): LoysingList {
    return resultat.map { it.loeysingId }.let { getLoeysingMap(it).getOrThrow() }
  }

}
