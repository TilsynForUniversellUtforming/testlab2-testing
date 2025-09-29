package no.uutilsynet.testlab2testing.aggregering

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestKoeyring
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.URL
import java.time.Instant

private const val AGGREGERING_URL_ER_NULL = "Aggregering url er null"

@Service
class AggregeringService(
    val autoTesterClient: AutoTesterClient,
    val kravregisterClient: KravregisterClient,
    val aggregeringDAO: AggregeringDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
    val testregelService: TestregelService,
    val maalingDAO: MaalingDAO,
    private val testgrunnlagService: TestgrunnlagService
) {

  private val logger = LoggerFactory.getLogger(AggregeringService::class.java)

  @Transactional
  fun saveAggregering(testKoeyring: TestKoeyring.Ferdig) {
    saveAggregertResultatTestregelAutomatisk(testKoeyring)
    saveAggregeringSideAutomatisk(testKoeyring)
    saveAggregertResultatSuksesskriteriumAutomatisk(testKoeyring)
  }

    fun <T, D> saveAggregertResultat(
        testKoeyring: TestKoeyring.Ferdig,
        urlExtractor: (AutoTesterClient.AutoTesterLenker?) -> URI?,
        resultType: AutoTesterClient.ResultatUrls,
        filterType: Class<T>,
        dtoMapper: (T) -> D,
        daoSaver: (D) -> Unit,
        logName: String
    ) {
        logger.info("Lagrer aggregert resultat for $logName for testkoeyring ${testKoeyring.loeysing.namn}")
        val aggregeringUrl = urlExtractor(testKoeyring.lenker) ?: throw RuntimeException(AGGREGERING_URL_ER_NULL)
        runCatching {
            autoTesterClient
                .fetchResultatAggregering(aggregeringUrl, resultType)
                .filterIsInstance(filterType)
                .map(dtoMapper)
                .forEach(daoSaver)
        }.onFailure {
            logger.error("Kunne ikkje lagre aggregert resultat for $logName for testkoeyring ${testKoeyring.loeysing.namn}", it)
            throw it
        }
    }

    fun saveAggregertResultatTestregelAutomatisk(testKoeyring: TestKoeyring.Ferdig) =
        saveAggregertResultat(
            testKoeyring,
            { it?.urlAggregeringTR?.toURI() },
            AutoTesterClient.ResultatUrls.urlAggreggeringTR,
            AggregertResultatTestregel::class.java,
            ::aggregertResultatTestregelToDTO,
            aggregeringDAO::createAggregertResultatTestregel,
            "testregel"
        )

    fun saveAggregertResultatSuksesskriteriumAutomatisk(testKoeyring: TestKoeyring.Ferdig) =
        saveAggregertResultat(
            testKoeyring,
            { it?.urlAggregeringSK?.toURI() },
            AutoTesterClient.ResultatUrls.urlAggregeringSK,
            AggregertResultatSuksesskriterium::class.java,
            ::aggregertResultatSuksesskritieriumToDTO,
            aggregeringDAO::createAggregertResultatSuksesskriterium,
            "suksesskriterium"
        )

    fun saveAggregeringSideAutomatisk(testKoeyring: TestKoeyring.Ferdig) =
        saveAggregertResultat(
            testKoeyring,
            { it?.urlAggregeringSide?.toURI() },
            AutoTesterClient.ResultatUrls.urlAggregeringSide,
            AggregertResultatSide::class.java,
            ::aggregerteResultatSideTODTO,
            aggregeringDAO::createAggregeringSide,
            "side"
        )


  fun aggregertResultatTestregelToDTO(
      aggregertResultatTestregel: AggregertResultatTestregel
  ): AggregeringPerTestregelDTO {

    val testregel = testregelService.getTestregelFromSchema(aggregertResultatTestregel.testregelId)

    return AggregeringPerTestregelDTO(
        aggregertResultatTestregel.maalingId,
        aggregertResultatTestregel.loeysing.id,
        testregel.id,
        testregel.kravId,
        aggregertResultatTestregel.fleireSuksesskriterium.map {
          kravregisterClient.getKravIdFromSuksesskritterium(it)
        },
        aggregertResultatTestregel.talElementSamsvar,
        aggregertResultatTestregel.talElementBrot,
        aggregertResultatTestregel.talElementVarsel,
        aggregertResultatTestregel.talElementIkkjeForekomst,
        aggregertResultatTestregel.talSiderSamsvar,
        aggregertResultatTestregel.talSiderBrot,
        aggregertResultatTestregel.talSiderIkkjeForekomst,
        aggregertResultatTestregel.testregelGjennomsnittlegSideSamsvarProsent,
        aggregertResultatTestregel.testregelGjennomsnittlegSideBrotProsent,
        null)
  }

  fun aggregerteResultatSideTODTO(
      aggregertResultatSide: AggregertResultatSide
  ): AggregeringPerSideDTO {
    return AggregeringPerSideDTO(
        aggregertResultatSide.maalingId,
        aggregertResultatSide.loeysing.id,
        aggregertResultatSide.sideUrl,
        aggregertResultatSide.sideNivaa,
        aggregertResultatSide.gjennomsnittligBruddProsentTR,
        aggregertResultatSide.talElementSamsvar,
        aggregertResultatSide.talElementBrot,
        aggregertResultatSide.talElementVarsel,
        aggregertResultatSide.talElementIkkjeForekomst,
        null)
  }

  fun aggregertResultatSuksesskritieriumToDTO(
      aggregertResultatSuksesskriterium: AggregertResultatSuksesskriterium
  ): AggregeringPerSuksesskriteriumDTO {

    return AggregeringPerSuksesskriteriumDTO(
        aggregertResultatSuksesskriterium.maalingId,
        aggregertResultatSuksesskriterium.loeysing.id,
        kravregisterClient.getKravIdFromSuksesskritterium(
            aggregertResultatSuksesskriterium.suksesskriterium),
        aggregertResultatSuksesskriterium.talSiderSamsvar,
        aggregertResultatSuksesskriterium.talSiderBrot,
        aggregertResultatSuksesskriterium.talSiderIkkjeForekomst,
        null)
  }

  fun dtoToAggregertResultatTestregel(
      aggregeringPerTestregelDTO: AggregeringPerTestregelDTO,
      loeysingList: List<Loeysing>
  ): AggregertResultatTestregelAPI {

    val id = aggregeringPerTestregelDTO.maalingId ?: aggregeringPerTestregelDTO.testgrunnlagId

    val testregel = getTestregel(aggregeringPerTestregelDTO.testregelId)

    return AggregertResultatTestregelAPI(
        id,
        getLoeysing(aggregeringPerTestregelDTO.loeysingId, loeysingList),
        testregel.testregelId,
        getSuksesskriterium(aggregeringPerTestregelDTO.suksesskriterium),
        aggregeringPerTestregelDTO.talElementSamsvar,
        aggregeringPerTestregelDTO.talElementBrot,
        aggregeringPerTestregelDTO.talElementVarsel,
        aggregeringPerTestregelDTO.talElementIkkjeForekomst,
        aggregeringPerTestregelDTO.talSiderSamsvar,
        aggregeringPerTestregelDTO.talSiderBrot,
        aggregeringPerTestregelDTO.talSiderIkkjeForekomst,
        aggregeringPerTestregelDTO.testregelGjennomsnittlegSideSamsvarProsent,
        aggregeringPerTestregelDTO.testregelGjennomsnittlegSideBrotProsent)
  }

  fun dtoToAggregertResultatSide(
      aggregeringPerSideDTO: AggregeringPerSideDTO,
      loeysingList: List<Loeysing>
  ): AggregertResultatSide {
    return AggregertResultatSide(
        aggregeringPerSideDTO.maalingId ?: aggregeringPerSideDTO.testgrunnlagId,
        getLoeysing(aggregeringPerSideDTO.loeysingId, loeysingList),
        aggregeringPerSideDTO.sideUrl,
        aggregeringPerSideDTO.sideNivaa,
        aggregeringPerSideDTO.gjennomsnittligBruddProsentTR,
        aggregeringPerSideDTO.talElementSamsvar,
        aggregeringPerSideDTO.talElementBrot,
        aggregeringPerSideDTO.talElementVarsel,
        aggregeringPerSideDTO.talElementIkkjeForekomst)
  }

  fun dtoTOAggregertResultatSuksesskriterium(
      aggregeringPerSuksesskriteriumDTO: AggregeringPerSuksesskriteriumDTO,
      loeysingList: List<Loeysing>
  ): AggregertResultatSuksesskriterium {
    return AggregertResultatSuksesskriterium(
        aggregeringPerSuksesskriteriumDTO.maalingId
            ?: aggregeringPerSuksesskriteriumDTO.testgrunnlagId,
        getLoeysing(aggregeringPerSuksesskriteriumDTO.loeysingId, loeysingList),
        getSuksesskriterium(aggregeringPerSuksesskriteriumDTO.suksesskriteriumId),
        aggregeringPerSuksesskriteriumDTO.talSiderSamsvar,
        aggregeringPerSuksesskriteriumDTO.talSiderBrot,
        aggregeringPerSuksesskriteriumDTO.talSiderIkkjeForekomst)
  }

  private fun getSuksesskriterium(suksesskriteriumId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(suksesskriteriumId)

  private fun getLoeysing(loeysingId: Int, loeysingList: List<Loeysing>): Loeysing =
      loeysingList.firstOrNull { it.id == loeysingId }
          ?: throw NoSuchElementException("Fant ikkje loeysing med id $loeysingId")

  fun getTestregel(testregelId: Int): Testregel {
    return testregelService.getTestregel(testregelId)
  }

  fun getAggregertResultatTestregel(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatTestregelAPI> {
    logger.info("Henter aggregert resultat for testregel med id ${maalingId?:testgrunnlagId}")
    if (maalingId != null) {
      val loeysingList = getLoeysingarForMaaling(maalingId)
      return aggregeringDAO.getAggregertResultatTestregelForMaaling(maalingId).map {
        dtoToAggregertResultatTestregel(it, loeysingList)
      }
    } else if (testgrunnlagId != null) {
      val loeysingList = getLoeysingarForTestgrunnlag(testgrunnlagId)
      return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId).map {
        dtoToAggregertResultatTestregel(it, loeysingList)
      }
    }
    return emptyList()
  }

  fun getAggregertResultatSide(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSide> {
    logger.info("Henter aggregering for resultat for side med id ${maalingId?:testgrunnlagId}")

    if (maalingId != null) {
      val loeysingList = getLoeysingarForMaaling(maalingId)
      return aggregeringDAO.getAggregertResultatSideForMaaling(maalingId).map {
        dtoToAggregertResultatSide(it, loeysingList)
      }
    } else if (testgrunnlagId != null) {
      val loeysingList = getLoeysingarForTestgrunnlag(testgrunnlagId)
      return aggregeringDAO.getAggregertResultatSideForTestgrunnlag(testgrunnlagId).map {
        dtoToAggregertResultatSide(it, loeysingList)
      }
    }
    return emptyList()
  }

  private fun getLoeysingarForTestgrunnlag(testgrunnlagId: Int) =
      testgrunnlagService.getLoeysingForTestgrunnlag(testgrunnlagId)

  private fun getLoeysingarForMaaling(maalingId: Int) =
      maalingDAO.getLoeysingarForMaaling(maalingId, Instant.now())

  fun getAggregertResultatSuksesskriterium(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSuksesskriterium> {
    if (maalingId != null) {
      val loeysingList = getLoeysingarForMaaling(maalingId)
      return aggregeringDAO.getAggregertResultatSuksesskriteriumForMaaling(maalingId).map {
        dtoTOAggregertResultatSuksesskriterium(it, loeysingList)
      }
    } else if (testgrunnlagId != null) {
      val loeysingList = getLoeysingarForTestgrunnlag(testgrunnlagId)
      return aggregeringDAO
          .getAggregertResultatSuksesskriteriumForTestgrunnlag(testgrunnlagId)
          .map { dtoTOAggregertResultatSuksesskriterium(it, loeysingList) }
    }
    return emptyList()
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
      dtoToAggregertResultatTestregel(it, loeysingList)
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
            if (result < 1) {
              throw RuntimeException(
                  "Kunne ikkje lagre aggregert resultat for testregel for testgrunnlag ${it.testgrunnlagId} og testregel ${it.testregelId}")
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
              createAggregeringPerSuksesskriteriumDTO(testresultatForSak)
          aggregertResultatSuksesskriterium.forEach {
            val result = aggregeringDAO.createAggregertResultatSuksesskriterium(it)
            if (result < 1) {
              throw RuntimeException(
                  "Kunne ikkje lagre aggregert resultat for testregel for testgrunnlag ${it.testgrunnlagId} og suksesskriterium ${it.suksesskriteriumId}")
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

  fun saveAggregertResultatSide(testresultatList: List<ResultatManuellKontroll>): Result<Boolean> =
      runCatching {
            val aggregertResultatSide = createAggregeringPerSideDTO(testresultatList)

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
  ): List<AggregeringPerTestregelDTO> {

    return testresultatForSak
        .groupBy { it.loeysingId }
        .entries
        .map { aggregeringPerTestregelDTOPrLoeysing(it.value) }
        .flatten()
  }

  private fun aggregeringPerTestregelDTOPrLoeysing(
      it: List<ResultatManuellKontroll>
  ): List<AggregeringPerTestregelDTO> {
    val testresultatForLoeysingPerTestregel =
        it.groupBy { it.testregelId }.entries.map { aggregeringPerTestregelDTO(it) }
    return testresultatForLoeysingPerTestregel
  }

  private fun aggregeringPerTestregelDTO(
      it: Map.Entry<Int, List<ResultatManuellKontroll>>
  ): AggregeringPerTestregelDTO {
    val testresultat = it.value
    val talElementUtfall = countElementUtfall(testresultat)

    val (talSiderBrot, talSiderSamsvar, talSiderIkkjeForekomst) = countSideUtfall(testresultat)

    val suksesskriterium = getKravIdFraTestregel(it.key)

    val gjennomsnittTestresultat = calculateTestregelGjennomsnitt(testresultat)

    return AggregeringPerTestregelDTO(
        null,
        testresultat.first().loeysingId,
        it.key,
        suksesskriterium,
        listOf(suksesskriterium),
        talElementUtfall.talSamsvar,
        talElementUtfall.talBrot,
        talElementUtfall.talVarsel,
        talElementUtfall.talIkkjeForekomst,
        talSiderSamsvar,
        talSiderBrot,
        talSiderIkkjeForekomst,
        gjennomsnittTestresultat.testregelGjennomsnittlegSideSamsvarProsent,
        gjennomsnittTestresultat.testregelGjennomsnittlegSideBrotProsent,
        testresultat.first().testgrunnlagId)
  }

  fun processPrSideutval(values: List<ResultatManuellKontroll>): ResultatPerTestregelPerSide {
    val talElementUtfall = countElementUtfall(values)

    val ikkjeForekomst = talElementUtfall.talIkkjeForekomst > 0

    if (ikkjeForekomst) {
      return ResultatPerTestregelPerSide(
          brotprosentTrSide = 0.0, samsvarsprosentTrSide = 0.0, ikkjeForekomst = true)
    }

    return ResultatPerTestregelPerSide(
        brotprosentTrSide =
            (talElementUtfall.talBrot.toDouble() /
                (talElementUtfall.talBrot + talElementUtfall.talSamsvar).toDouble()),
        samsvarsprosentTrSide =
            (talElementUtfall.talSamsvar.toDouble() /
                (talElementUtfall.talBrot + talElementUtfall.talSamsvar).toDouble()),
        ikkjeForekomst = false)
  }

  private fun countElementUtfall(values: List<ResultatManuellKontroll>): TalUtfall {
    val talElementBrot = values.count { it.elementResultat == TestresultatUtfall.brot }
    val talElementSamsvar = values.count { it.elementResultat == TestresultatUtfall.samsvar }
    val talElementIkkjeForekomst =
        values.count { it.elementResultat == TestresultatUtfall.ikkjeForekomst }
    val talElementVarsel = values.count { it.elementResultat == TestresultatUtfall.varsel }
    val talElementIkkjeTesta = values.count { it.elementResultat == TestresultatUtfall.ikkjeTesta }
    return TalUtfall(
        talBrot = talElementBrot,
        talSamsvar = talElementSamsvar,
        talIkkjeForekomst = talElementIkkjeForekomst,
        talVarsel = talElementVarsel,
        talIkkjeTesta = talElementIkkjeTesta)
  }

  fun calculateTestregelGjennomsnitt(
      values: List<ResultatManuellKontroll>
  ): GjennomsnittTestresultat {
    val resultatPerTestregelPerSide: List<ResultatPerTestregelPerSide> =
        values.groupBy { it.sideutvalId }.entries.map { processPrSideutval(it.value) }

    return gjennomsnittTestresultat(resultatPerTestregelPerSide)
  }

  private fun gjennomsnittTestresultat(
      resultatPerTestregelPerSide: List<ResultatPerTestregelPerSide>
  ): GjennomsnittTestresultat {
    var talSiderMedForekomst = 0
    var summertBrotprosent = 0.0
    var summertSamsvarprosent = 0.0

    resultatPerTestregelPerSide.forEach {
      summertBrotprosent += addIfNotIkkjeForekomst(it.brotprosentTrSide, it.ikkjeForekomst)
      summertSamsvarprosent += addIfNotIkkjeForekomst(it.samsvarsprosentTrSide, it.ikkjeForekomst)
      talSiderMedForekomst += addIfNotIkkjeForekomst(1.0, it.ikkjeForekomst).toInt()
    }
    val testregelGjennomsnittlegSideBrot =
        (summertBrotprosent / talSiderMedForekomst).takeUnless { it.isNaN() }
    val testregelGjennomsnittlegSideSamsvar =
        (summertSamsvarprosent / talSiderMedForekomst).takeUnless { it.isNaN() }

    return GjennomsnittTestresultat(
        testregelGjennomsnittlegSideSamsvar, testregelGjennomsnittlegSideBrot)
  }

  private fun addIfNotIkkjeForekomst(value: Double, ikkjeForekomst: Boolean): Double {
    return value.takeIf { !ikkjeForekomst } ?: 0.0
  }

  private fun createAggregeringPerSuksesskriteriumDTO(
      testresultatForSak: List<ResultatManuellKontroll>
  ): List<AggregeringPerSuksesskriteriumDTO> {
    return testresultatForSak
        .groupBy { it.kravId() }
        .entries
        .map {
          val testresultat = it.value
          val (talSiderBrot, talSiderSamsvar, talSiderIkkjeForekomst) =
              countSideUtfall(testresultat)
          AggregeringPerSuksesskriteriumDTO(
              null,
              testresultat.first().loeysingId,
              testresultat.first().kravId(),
              talSiderSamsvar,
              talSiderBrot,
              talSiderIkkjeForekomst,
              testresultat.first().testgrunnlagId)
        }
  }

  private fun createAggregeringPerSideDTO(
      testresultatList: List<ResultatManuellKontroll>
  ): List<AggregeringPerSideDTO> {
    val testresultatMap = testresultatList.groupBy { it.sideutvalId }

    val sideutvalIdUrlMap: Map<Int, URL> =
        sideutvalDAO.getSideutvalUrlMapKontroll(testresultatMap.keys.toList())

    // Alle sideutvalIder skal referere til en gyldig url
    require(testresultatMap.keys.containsAll(sideutvalIdUrlMap.keys)) {
      "Ugyldige nettsider i testresultat"
    }

    return testresultatMap
        .mapKeys { sideutvalIdUrlMap[it.key] }
        .filterKeys { it != null }
        .entries
        .map { entry ->
          requireNotNull(entry.key)
          val testresultat = entry.value

          AggregeringPerSideDTO(
              null,
              testresultat.first().loeysingId,
              entry.key!!,
              entry.key!!.path.split("/").size,
              0.0,
              testresultat.count { it.elementResultat == TestresultatUtfall.samsvar },
              testresultat.count { it.elementResultat == TestresultatUtfall.brot },
              0,
              testresultat.count { it.elementResultat == TestresultatUtfall.ikkjeForekomst },
              testresultat.first().testgrunnlagId)
        }
  }

  private fun countSideUtfall(testresultat: List<ResultatManuellKontroll>): TalUtfall {
    var talSiderBrot = 0
    var talSiderSamsvar = 0
    var talSiderIkkjeForekomst = 0
    var talSiderVarsel = 0
    var talSiderIkkjeTesta = 0

    testresultat
        .groupBy { it.sideutvalId }
        .entries
        .forEach { _ ->
          when (calculateUtfall(testresultat.map { it.elementResultat })) {
            TestresultatUtfall.brot -> talSiderBrot += 1
            TestresultatUtfall.samsvar -> talSiderSamsvar += 1
            TestresultatUtfall.ikkjeForekomst -> talSiderIkkjeForekomst += 1
            TestresultatUtfall.varsel -> talSiderVarsel += 1
            TestresultatUtfall.ikkjeTesta -> talSiderIkkjeTesta += 1
          }
        }
    return TalUtfall(
        talBrot = talSiderBrot,
        talSamsvar = talSiderSamsvar,
        talIkkjeForekomst = talSiderIkkjeForekomst,
        talVarsel = talSiderVarsel,
        talIkkjeTesta = talSiderIkkjeTesta)
  }

  fun getKravIdFraTestregel(id: Int): Int {
    return getTestregel(id).kravId
  }

  fun calculateUtfall(utfall: List<TestresultatUtfall?>): TestresultatUtfall {
    if (utfall.contains(TestresultatUtfall.brot)) {
      return TestresultatUtfall.brot
    }
    if (utfall.contains(TestresultatUtfall.varsel)) {
      return TestresultatUtfall.varsel
    }
    if (utfall.contains(TestresultatUtfall.samsvar)) {
      return TestresultatUtfall.samsvar
    }
    return TestresultatUtfall.ikkjeForekomst
  }

  fun ResultatManuellKontroll.kravId(): Int {
    return getKravIdFraTestregel(this.testregelId)
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
