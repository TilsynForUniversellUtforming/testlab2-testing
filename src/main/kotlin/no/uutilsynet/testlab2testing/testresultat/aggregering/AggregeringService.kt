package no.uutilsynet.testlab2testing.testresultat.aggregering

import java.net.URI
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingReadService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testresultat.aggregering.mappers.AggregeringFromDTOMapper
import no.uutilsynet.testlab2testing.testresultat.aggregering.mappers.AggregeringToDTOMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val AGGREGERING_URL_ER_NULL = "Aggregering url er null"

@Suppress("LongParameterList")
@Service
class AggregeringService(
    private val autoTesterClient: AutoTesterClient,
    private val aggregeringDAO: AggregeringDAO,
    private val testResultatDAO: TestResultatDAO,
    private val maalingReadService: MaalingReadService,
    private val testgrunnlagService: TestgrunnlagService,
    private val aggregeringToDTOMapper: AggregeringToDTOMapper,
    private val aggregeringFromDTOMapper: AggregeringFromDTOMapper
) {
  private val logger = LoggerFactory.getLogger(AggregeringService::class.java)

  @Transactional
  fun saveAggregering(lenker: AutoTesterClient.AutoTesterLenker, loeysingId: Int) {
    saveAggregertResultatTestregelAutomatisk(lenker, loeysingId)
    saveAggregeringSideAutomatisk(lenker, loeysingId)
    saveAggregertResultatSuksesskriteriumAutomatisk(lenker, loeysingId)
  }

  @Suppress("LongParameterList")
  private fun <T, D> saveAggregertResultat(
      urlExtractor: (AutoTesterClient.AutoTesterLenker?) -> URI?,
      resultType: AutoTesterClient.ResultatUrls,
      filterType: Class<T>,
      dtoMapper: (T) -> D,
      daoSaver: (D) -> Unit,
      logName: String,
      lenker: AutoTesterClient.AutoTesterLenker,
      loeysingId: Int
  ) {
    logger.info("Lagrer aggregert resultat for $logName for testkoeyring $loeysingId")
    val aggregeringUrl =
        urlExtractor(lenker) ?: throw IllegalArgumentException(AGGREGERING_URL_ER_NULL)
    runCatching {
          autoTesterClient
              .fetchResultatAggregering(aggregeringUrl, resultType)
              .filterIsInstance(filterType)
              .map(dtoMapper)
              .forEach(daoSaver)
        }
        .onFailure {
          logger.error(
              "Kunne ikkje lagre aggregert resultat for $logName for testkoeyring $loeysingId", it)
          throw it
        }
  }

  fun saveAggregertResultatTestregelAutomatisk(
      lenker: AutoTesterClient.AutoTesterLenker,
      loeysingId: Int
  ) =
      saveAggregertResultat(
          { it?.urlAggregeringTR?.toURI() },
          AutoTesterClient.ResultatUrls.urlAggreggeringTR,
          AggregertResultatTestregel::class.java,
          aggregeringToDTOMapper::aggregertResultatTestregelToDTO,
          aggregeringDAO::createAggregertResultatTestregel,
          "testregel",
          lenker,
          loeysingId)

  fun saveAggregertResultatSuksesskriteriumAutomatisk(
      lenker: AutoTesterClient.AutoTesterLenker,
      loeysingId: Int
  ) =
      saveAggregertResultat(
          { it?.urlAggregeringSK?.toURI() },
          AutoTesterClient.ResultatUrls.urlAggregeringSK,
          AggregertResultatSuksesskriterium::class.java,
          aggregeringToDTOMapper::aggregertResultatSuksesskritieriumToDTO,
          aggregeringDAO::createAggregertResultatSuksesskriterium,
          "suksesskriterium",
          lenker,
          loeysingId)

  fun saveAggregeringSideAutomatisk(lenker: AutoTesterClient.AutoTesterLenker, loeysingId: Int) =
      saveAggregertResultat(
          { it?.urlAggregeringSide?.toURI() },
          AutoTesterClient.ResultatUrls.urlAggregeringSide,
          AggregertResultatSide::class.java,
          aggregeringToDTOMapper::aggregerteResultatSideTODTO,
          aggregeringDAO::createAggregeringSide,
          "side",
          lenker,
          loeysingId)

  fun getAggregertResultatTestregel(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatTestregelAPI> {
    logger.info("Henter aggregert resultat for testregel med id ${maalingId ?: testgrunnlagId}")
    return getAggregertResultat(
        maalingId = maalingId,
        testgrunnlagId = testgrunnlagId,
        fetchForMaaling = aggregeringDAO::getAggregertResultatTestregelForMaaling,
        fetchForTestgrunnlag = aggregeringDAO::getAggregertResultatTestregelForTestgrunnlag,
        mapResults = aggregeringFromDTOMapper::dtoToAggregertResultatTestregel)
  }

  fun getAggregertResultatSide(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSide> {
    logger.info("Henter aggregert resultat for side med id ${maalingId ?: testgrunnlagId}")
    return getAggregertResultat(
        maalingId = maalingId,
        testgrunnlagId = testgrunnlagId,
        fetchForMaaling = aggregeringDAO::getAggregertResultatSideForMaaling,
        fetchForTestgrunnlag = aggregeringDAO::getAggregertResultatSideForTestgrunnlag,
        mapResults = aggregeringFromDTOMapper::dtoToAggregertResultatSide)
  }

  fun getAggregertResultatSuksesskriterium(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSuksesskriterium> {
    logger.info(
        "Henter aggregert resultat for suksesskriterium med id ${maalingId ?: testgrunnlagId}")
    return getAggregertResultat(
        maalingId = maalingId,
        testgrunnlagId = testgrunnlagId,
        fetchForMaaling = aggregeringDAO::getAggregertResultatSuksesskriteriumForMaaling,
        fetchForTestgrunnlag = aggregeringDAO::getAggregertResultatSuksesskriteriumForTestgrunnlag,
        mapResults = aggregeringFromDTOMapper::dtoTOAggregertResultatSuksesskriterium)
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {
    return aggregeringDAO.harMaalingLagraAggregering(maalingId, aggregeringstype)
  }

  fun getAggregertResultatTestregelForTestgrunnlag(
      testgrunnlagId: Int
  ): List<AggregertResultatTestregelAPI> {
    logger.info("Henter aggregert resultat for testgrunnlag med id $testgrunnlagId")
    val loeysingList = testgrunnlagService.getLoeysingForTestgrunnlag(testgrunnlagId)
    return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId).map {
      aggregeringFromDTOMapper.dtoToAggregertResultatTestregel(it, loeysingList)
    }
  }

  @Transactional
  fun saveAggregertResultat(testgrunnlagId: Int): Result<Boolean> {
    return toBooleanResult {
      val testresultatList =
          testResultatDAO.getManyResults(testgrunnlagId = testgrunnlagId).getOrThrow()
      saveAggregertResultatTestregel(testresultatList).getOrThrow()
      saveAggregertResultatSuksesskriterium(testresultatList).getOrThrow()
      saveAggregertResultatSide(testresultatList).getOrThrow()
    }
  }

  @Transactional
  fun saveAggregertResultatTestregel(
      testresultatForSak: List<ResultatManuellKontroll>
  ): Result<Boolean> {
    return toBooleanResult {
      val aggregertResultatTestregel =
          aggregeringToDTOMapper.createAggregeringPerTestregelDTO(testresultatForSak)
      aggregertResultatTestregel.forEach {
        val result = aggregeringDAO.createAggregertResultatTestregel(it)
        check(result > 0) {
          "Kunne ikkje lagre aggregert resultat for testregel for testgrunnlag " +
              "${it.testgrunnlagId} og testregel ${it.testregelId}"
        }
      }
    }
  }

  @Transactional
  fun saveAggregertResultatSuksesskriterium(
      testresultatForSak: List<ResultatManuellKontroll>
  ): Result<Boolean> {
    return toBooleanResult {
      val aggregertResultatSuksesskriterium =
          aggregeringToDTOMapper.createAggregeringPerSuksesskriteriumDTO(testresultatForSak)
      aggregertResultatSuksesskriterium.forEach {
        val result = aggregeringDAO.createAggregertResultatSuksesskriterium(it)
        check(result > 0) {
          "Kunne ikkje lagre aggregert resultat for testregel " +
              "for testgrunnlag ${it.testgrunnlagId} og suksesskriterium ${it.suksesskriteriumId}"
        }
      }
    }
  }

  fun saveAggregertResultatSide(testresultatList: List<ResultatManuellKontroll>): Result<Boolean> =
      toBooleanResult {
        val aggregertResultatSide =
            aggregeringToDTOMapper.createAggregeringPerSideDTO(testresultatList)
        aggregertResultatSide.forEach { aggregeringDAO.createAggregeringSide(it).getOrThrow() }
      }

  private inline fun toBooleanResult(operation: () -> Unit): Result<Boolean> {
    return runCatching(operation).map { true }
  }

  private fun <T, R> getAggregertResultat(
      maalingId: Int?,
      testgrunnlagId: Int?,
      fetchForMaaling: (Int) -> List<T>,
      fetchForTestgrunnlag: (Int) -> List<T>,
      mapResults: (T, List<Loeysing>) -> R
  ): List<R> {
    val id = maalingId ?: testgrunnlagId ?: return emptyList()
    val loeysingList =
        if (maalingId != null) {
          maalingReadService.getLoeysingarForMaaling(id)
        } else {
          testgrunnlagService.getLoeysingForTestgrunnlag(id)
        }
    val resultater = if (maalingId != null) fetchForMaaling(id) else fetchForTestgrunnlag(id)
    return resultater.map { mapResults(it, loeysingList) }
  }
}

data class GjennomsnittTestresultat(
    val testregelGjennomsnittlegSideSamsvarProsent: Double?,
    val testregelGjennomsnittlegSideBrotProsent: Double?
)

data class ResultatPerTestregelPerSide(
    val brotprosentTrSide: Double,
    val samsvarsprosentTrSide: Double,
    val ikkjeForekomst: Boolean
)
