package no.uutilsynet.testlab2testing.aggregering

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.dto.TestresultatUtfall
import no.uutilsynet.testlab2testing.forenkletkontroll.AutoTesterClient
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.TestKoeyring
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AggregeringService(
    val autoTesterClient: AutoTesterClient,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val kravregisterClient: KravregisterClient,
    val testregelDAO: TestregelDAO,
    val aggregeringDAO: AggregeringDAO,
    val testResultatDAO: TestResultatDAO,
    val sideutvalDAO: SideutvalDAO,
) {

  private val logger = LoggerFactory.getLogger(AggregeringService::class.java)

  @Transactional
  fun saveAggregering(testKoeyring: TestKoeyring.Ferdig) {
    saveAggregertResultatTestregelAutomatisk(testKoeyring)
    saveAggregeringSideAutomatisk(testKoeyring)
    saveAggregertResultatSuksesskriteriumAutomatisk(testKoeyring)
  }

  fun saveAggregertResultatTestregelAutomatisk(testKoeyring: TestKoeyring.Ferdig) {
    logger.info(
        "Lagrer aggregert resultat for testregel for testkoeyring ${testKoeyring.loeysing.namn}}")
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringTR?.toURI()
    runCatching {
          aggregeringUrl?.let {
            val aggregertResultatTestregel: List<AggregertResultatTestregel> =
                autoTesterClient.fetchResultatAggregering(
                    aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggreggeringTR)
                    as List<AggregertResultatTestregel>

            aggregertResultatTestregel
                .map { aggregertResultatTestregelToDTO(it) }
                .forEach { aggregeringDAO.createAggregertResultatTestregel(it) }
          }
        }
        .onFailure {
          logger.error(
              "Kunne ikkje lagre aggregert resultat for testregel for testkoeyring ${testKoeyring.loeysing.namn}",
              it)
          throw it
        }
  }

  fun saveAggregeringSideAutomatisk(testKoeyring: TestKoeyring.Ferdig) {
    logger.info(
        "Lagrer aggregert resultat for side for testkoeyring ${testKoeyring.loeysing.namn}}")
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringSide?.toURI()

    runCatching {
          aggregeringUrl?.let {
            val aggregeringSide: List<AggregertResultatSide> =
                autoTesterClient.fetchResultatAggregering(
                    aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggregeringSide)
                    as List<AggregertResultatSide>

            aggregeringSide
                .map { aggregertResultatSide -> aggregerteResultatSideTODTO(aggregertResultatSide) }
                .forEach { aggregeringDAO.createAggregeringSide(it) }
          }
              ?: throw RuntimeException("Aggregering url er null")
        }
        .onFailure {
          logger.error(
              "Kunne ikkje lagre aggregert resultat for side for testkoeyring ${testKoeyring.loeysing.namn}",
              it)
          throw it
        }
  }

  fun saveAggregertResultatSuksesskriteriumAutomatisk(testKoeyring: TestKoeyring.Ferdig) {
    logger.info(
        "Lagrer aggregert resultat for suksesskriterium for testkoeyring ${testKoeyring.loeysing.namn}}")
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringSK?.toURI()

    runCatching {
          aggregeringUrl?.let {
            val aggregertResultatSuksesskriterium: List<AggregertResultatSuksesskriterium> =
                autoTesterClient.fetchResultatAggregering(
                    aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggregeringSK)
                    as List<AggregertResultatSuksesskriterium>

            aggregertResultatSuksesskriterium
                .map { aggregertResultatSuksesskritieriumToDTO(it) }
                .forEach { aggregeringDAO.createAggregertResultatSuksesskriterium(it) }
          }
              ?: throw RuntimeException("Aggregering url er null")
        }
        .onFailure {
          logger.error(
              "Kunne ikkje lagre aggregert resultat for suksesskriterium for testkoeyring ${testKoeyring.loeysing.namn}",
              it)
          throw it
        }
  }

  fun aggregertResultatTestregelToDTO(
      aggregertResultatTestregel: AggregertResultatTestregel
  ): AggregeringPerTestregelDTO {

    return AggregeringPerTestregelDTO(
        aggregertResultatTestregel.maalingId,
        aggregertResultatTestregel.loeysing.id,
        getTestregelIdFromSchema(aggregertResultatTestregel.testregelId).let { testregelId ->
          testregelId
              ?: throw RuntimeException(
                  "Fant ikkje testregel med testregeId ${aggregertResultatTestregel.testregelId}")
        },
        kravregisterClient.getKravIdFromSuksesskritterium(
            aggregertResultatTestregel.suksesskriterium),
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
      aggregeringPerTestregelDTO: AggregeringPerTestregelDTO
  ): AggregertResultatTestregel {

    val id = aggregeringPerTestregelDTO.maalingId ?: aggregeringPerTestregelDTO.testgrunnlagId

    return AggregertResultatTestregel(
        id,
        getLoeysing(aggregeringPerTestregelDTO.loeysingId),
        getTestregelId(aggregeringPerTestregelDTO.testregelId),
        getSuksesskriterium(aggregeringPerTestregelDTO.suksesskriterium),
        aggregeringPerTestregelDTO.fleireSuksesskriterium.map { getSuksesskriterium(it.toInt()) },
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
      aggregeringPerSideDTO: AggregeringPerSideDTO
  ): AggregertResultatSide {
    return AggregertResultatSide(
        aggregeringPerSideDTO.maalingId ?: aggregeringPerSideDTO.testgrunnlagId,
        getLoeysing(aggregeringPerSideDTO.loeysingId),
        aggregeringPerSideDTO.sideUrl,
        aggregeringPerSideDTO.sideNivaa,
        aggregeringPerSideDTO.gjennomsnittligBruddProsentTR,
        aggregeringPerSideDTO.talElementSamsvar,
        aggregeringPerSideDTO.talElementBrot,
        aggregeringPerSideDTO.talElementVarsel,
        aggregeringPerSideDTO.talElementIkkjeForekomst)
  }

  fun dtoTOAggregertResultatSuksesskriterium(
      aggregeringPerSuksesskriteriumDTO: AggregeringPerSuksesskriteriumDTO
  ): AggregertResultatSuksesskriterium {
    return AggregertResultatSuksesskriterium(
        aggregeringPerSuksesskriteriumDTO.maalingId
            ?: aggregeringPerSuksesskriteriumDTO.testgrunnlagId,
        getLoeysing(aggregeringPerSuksesskriteriumDTO.loeysingId),
        getSuksesskriterium(aggregeringPerSuksesskriteriumDTO.suksesskriteriumId),
        aggregeringPerSuksesskriteriumDTO.talSiderSamsvar,
        aggregeringPerSuksesskriteriumDTO.talSiderBrot,
        aggregeringPerSuksesskriteriumDTO.talSiderIkkjeForekomst)
  }

  private fun getSuksesskriterium(suksesskriteriumId: Int) =
      kravregisterClient.getSuksesskriteriumFromKrav(suksesskriteriumId)

  private fun getLoeysing(loeysingId: Int): Loeysing =
      loeysingsRegisterClient.getLoeysingFromId(loeysingId)

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  fun getTestregelId(idTestregel: Int): String {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel?.testregelId
          ?: throw RuntimeException("Fant ikkje testregel med id $idTestregel")
    }
  }

  fun getAggregertResultatTestregel(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatTestregel> {
    logger.info("Henter aggregert resultat for testregel med id ${maalingId?:testgrunnlagId}")
    if (maalingId != null) {
      return aggregeringDAO.getAggregertResultatTestregelForMaaling(maalingId).map {
        dtoToAggregertResultatTestregel(it)
      }
    } else if (testgrunnlagId != null) {
      return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId).map {
        dtoToAggregertResultatTestregel(it)
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
      return aggregeringDAO.getAggregertResultatSideForMaaling(maalingId).map {
        dtoToAggregertResultatSide(it)
      }
    } else if (testgrunnlagId != null) {
      return aggregeringDAO.getAggregertResultatSideForTestgrunnlag(testgrunnlagId).map {
        dtoToAggregertResultatSide(it)
      }
    }
    return emptyList()
  }

  fun getAggregertResultatSuksesskriterium(
      maalingId: Int? = null,
      testgrunnlagId: Int? = null
  ): List<AggregertResultatSuksesskriterium> {
    if (maalingId != null) {
      return aggregeringDAO.getAggregertResultatSuksesskriteriumForMaaling(maalingId).map {
        dtoTOAggregertResultatSuksesskriterium(it)
      }
    } else if (testgrunnlagId != null) {
      return aggregeringDAO
          .getAggregertResultatSuksesskriteriumForTestgrunnlag(testgrunnlagId)
          .map { dtoTOAggregertResultatSuksesskriterium(it) }
    }
    return emptyList()
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {
    return aggregeringDAO.harMaalingLagraAggregering(maalingId, aggregeringstype)
  }

  fun getAggregertResultatTestregelForTestgrunnlag(
      testgrunnlagId: Int
  ): List<AggregertResultatTestregel> {
    logger.info("Henter aggregert resultat for testgrunnlag med id $testgrunnlagId")
    return aggregeringDAO.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId).map {
      dtoToAggregertResultatTestregel(it)
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
    var talSiderMedForekomst: Int = 0
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
    if (!testresultatMap.keys.containsAll(sideutvalIdUrlMap.keys)) {
      throw IllegalArgumentException("Ugyldige nettsider i testresultat")
    }

    return testresultatMap.entries.map { entry ->
      val sideUrl = sideutvalIdUrlMap[entry.key]!!
      val testresultat = entry.value

      AggregeringPerSideDTO(
          null,
          testresultat.first().loeysingId,
          sideUrl,
          sideUrl.path.split("/").size,
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
    return testregelDAO.getTestregel(id)?.kravId
        ?: throw RuntimeException("Fant ikkje krav for testregel med id $id")
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
    return testregelDAO.getTestregel(this.testregelId)?.kravId
        ?: throw RuntimeException("Fant ikkje krav for testregel med id ${this.testregelId}")
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
