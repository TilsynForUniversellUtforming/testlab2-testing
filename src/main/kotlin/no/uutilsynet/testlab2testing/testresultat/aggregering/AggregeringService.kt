package no.uutilsynet.testlab2testing.testresultat.aggregering

import java.net.URI
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingReadService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
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
    private val testregelCache: TestregelCache,
    private val aggregeringToDTOMapper: AggregeringToDTOMapper,
    private val aggregeringFromDTOMapper: AggregeringFromDTOMapper
) {

  private val logger = LoggerFactory.getLogger(AggregeringService::class.java)

  @Transactional
  fun saveAggregering(lenker: AutoTesterClient.AutoTesterLenker, loeysingId: Int) {
    saveAggregertResultatTestregelAutomatisk(lenker,loeysingId)
    saveAggregeringSideAutomatisk(lenker,loeysingId)
    saveAggregertResultatSuksesskriteriumAutomatisk(lenker,loeysingId)
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
    logger.info(
        "Lagrer aggregert resultat for $logName for testkoeyring $loeysingId")
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
              "Kunne ikkje lagre aggregert resultat for $logName for testkoeyring $loeysingId",
              it)
          throw it
        }
  }

  fun saveAggregertResultatTestregelAutomatisk( lenker: AutoTesterClient.AutoTesterLenker, loeysingId: Int) =
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
      lenker: AutoTesterClient.AutoTesterLenker, loeysingId: Int
  ) =
      saveAggregertResultat(
          { it?.urlAggregeringSK?.toURI() },
          AutoTesterClient.ResultatUrls.urlAggregeringSK,
          AggregertResultatSuksesskriterium::class.java,
          aggregeringToDTOMapper::aggregertResultatSuksesskritieriumToDTO,
          aggregeringDAO::createAggregertResultatSuksesskriterium,
          "suksesskriterium",
          lenker,
          loeysingId
      )

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
    logger.info("Henter aggregert resultat for testregel med id ${maalingId?:testgrunnlagId}")
    val id = maalingId ?: testgrunnlagId ?: return emptyList()
      val loeysingList =
          getLoeysingList(maalingId, id)

      val resultater =
        if (maalingId != null) aggregeringDAO.getAggregertResultatTestregelForMaaling(id)
        else aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(id)
    return resultater.map { aggregeringFromDTOMapper.dtoToAggregertResultatTestregel(it, loeysingList) }
  }

  fun getAggregertResultatSide(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSide> {
    logger.info("Henter aggregering for resultat for side med id ${maalingId?:testgrunnlagId}")
    val id = maalingId ?: testgrunnlagId ?: return emptyList()

      val loeysingList =
          getLoeysingList(maalingId, id)

      return if (maalingId != null)
        aggregeringDAO.getAggregertResultatSideForMaaling(id).map {
            aggregeringFromDTOMapper.dtoToAggregertResultatSide(it, loeysingList)
        }
    else
        aggregeringDAO.getAggregertResultatSideForTestgrunnlag(id).map {
            aggregeringFromDTOMapper.dtoToAggregertResultatSide(it, loeysingList)
        }
  }

    private fun getLoeysingList(
        maalingId: Int?,
        id: Int
    ): List<Loeysing> {
        val loeysingList =
            if (maalingId != null) getLoeysingarForMaaling(id) else getLoeysingarForTestgrunnlag(id)
        return loeysingList
    }

    private fun getLoeysingarForTestgrunnlag(testgrunnlagId: Int) =
      testgrunnlagService.getLoeysingForTestgrunnlag(testgrunnlagId)

  private fun getLoeysingarForMaaling(maalingId: Int) =
      maalingReadService.getLoeysingarForMaaling(maalingId)

  fun getAggregertResultatSuksesskriterium(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSuksesskriterium> {
      return when {
          maalingId != null -> {
              val loeysingList = getLoeysingarForMaaling(maalingId)
              aggregeringDAO.getAggregertResultatSuksesskriteriumForMaaling(maalingId).map {
                  aggregeringFromDTOMapper.dtoTOAggregertResultatSuksesskriterium(it, loeysingList)
              }
          }

          testgrunnlagId != null -> {
              val loeysingList = getLoeysingarForTestgrunnlag(testgrunnlagId)
              aggregeringDAO
                  .getAggregertResultatSuksesskriteriumForTestgrunnlag(testgrunnlagId)
                  .map { aggregeringFromDTOMapper.dtoTOAggregertResultatSuksesskriterium(it, loeysingList) }
          }

          else -> emptyList()
      }
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {
    return aggregeringDAO.harMaalingLagraAggregering(maalingId, aggregeringstype)
  }

  fun getAggregertResultatTestregelForTestgrunnlag(
      testgrunnlagId: Int
  ): List<AggregertResultatTestregelAPI> {
    logger.info("Henter aggregert resultat for testgrunnlag med id $testgrunnlagId")
    val loeysingList = getLoeysingarForTestgrunnlag(testgrunnlagId)
    return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId).map {
        aggregeringFromDTOMapper.dtoToAggregertResultatTestregel(it, loeysingList)
    }
  }

  @Transactional
  fun saveAggregertResultat(testgrunnlagId: Int): Result<Boolean> {

    runCatching {
          val testresultatList =
              testResultatDAO.getManyResults(testgrunnlagId = testgrunnlagId).getOrThrow()

          saveAggregertResultatTestregel(testresultatList).getOrThrow()
          saveAggregertResultatSuksesskriterium(testresultatList).getOrThrow()
          saveAggregertResultatSide(testresultatList).getOrThrow()
        }
        .fold(
            onSuccess = {
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  @Transactional
  fun saveAggregertResultatTestregel(
      testresultatForSak: List<ResultatManuellKontroll>
  ): Result<Boolean> {
      runCatching {
          val aggregertResultatTestregel = createAggregeringPerTestregelDTO(testresultatForSak)
          aggregertResultatTestregel.forEach {
              val result = aggregeringDAO.createAggregertResultatTestregel(it)
              check(result > 0) {
                  "Kunne ikkje lagre aggregert resultat for testregel for testgrunnlag" +
                          " ${it.testgrunnlagId} og testregel ${it.testregelId}"
              }

          }
      }
        .fold(
            onSuccess = {
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  @Transactional
  fun saveAggregertResultatSuksesskriterium(
      testresultatForSak: List<ResultatManuellKontroll>
  ): Result<Boolean> {
    runCatching {
          val aggregertResultatSuksesskriterium =
              aggregeringToDTOMapper.createAggregeringPerSuksesskriteriumDTO(testresultatForSak)
          aggregertResultatSuksesskriterium.forEach {
            val result = aggregeringDAO.createAggregertResultatSuksesskriterium(it)
            check(result > 0) {"Kunne ikkje lagre aggregert resultat for testregel " +
                    "for testgrunnlag ${it.testgrunnlagId} og suksesskriterium ${it.suksesskriteriumId}"}
          }
        }
        .fold(
            onSuccess = {
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun saveAggregertResultatSide(testresultatList: List<ResultatManuellKontroll>): Result<Boolean> =
      runCatching {
            val aggregertResultatSide = aggregeringToDTOMapper.createAggregeringPerSideDTO(testresultatList)

            aggregertResultatSide.forEach { aggregeringDAO.createAggregeringSide(it).getOrThrow() }
          }
          .fold(
              onSuccess = {
                return Result.success(true)
              },
              onFailure = {
                return Result.failure(it)
              })

  private fun createAggregeringPerTestregelDTO(
      testresultatForSak: List<ResultatManuellKontroll>
  ): List<AggregeringPerTestregelDB> {
    return testresultatForSak
        .groupBy { it.loeysingId }
        .entries.flatMap { aggregeringPerTestregelDTOPrLoeysing(it.value) }
  }

  private fun aggregeringPerTestregelDTOPrLoeysing(
      it: List<ResultatManuellKontroll>
  ): List<AggregeringPerTestregelDB> {
    return it.groupBy { it.testregelId }.entries.map { aggregeringToDTOMapper.aggregeringPerTestregelDB(it) }
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
