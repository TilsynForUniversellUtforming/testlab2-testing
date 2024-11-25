package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.*
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.crawlParametersRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.deleteMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.insertMaalingLoeysingQuery
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.insertMaalingTestregelQuery
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByDateSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByIdSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.updateMaalingParams
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.updateMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.*
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Component
class MaalingDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val sideutvalDAO: SideutvalDAO,
    val brukarService: BrukarService,
    val cacheManager: CacheManager
) {

  private val logger = LoggerFactory.getLogger(MaalingDAO::class.java)

  data class MaalingDTO(
      val id: Int,
      val navn: String,
      val datoStart: Instant,
      val status: MaalingStatus,
      val maxLenker: Int,
      val talLenker: Int
  )

  object MaalingParams {
    val maalingRowmapper = DataClassRowMapper.newInstance(MaalingDTO::class.java)
    val crawlParametersRowmapper = DataClassRowMapper.newInstance(CrawlParameters::class.java)

    val createMaalingSql =
        """
      insert into "testlab2_testing"."maalingv1" (navn, status, dato_start, max_lenker, tal_lenker, utval_id) 
      values (:navn, :status, :dato_start, :max_lenker, :tal_lenker, :utvalId)
    """
            .trimIndent()

    fun createMaalingParams(
        navn: String,
        datoStart: Instant,
        crawlParameters: CrawlParameters,
        utvalId: UtvalId? = null
    ) =
        mapOf(
            "navn" to navn,
            "dato_start" to Timestamp.from(datoStart),
            "status" to "planlegging",
            "max_lenker" to crawlParameters.maxLenker,
            "tal_lenker" to crawlParameters.talLenker,
            "utvalId" to utvalId)

    val selectMaalingSql =
        "select id, navn, dato_start, status, max_lenker, tal_lenker from Maalingv1"

    val selectMaalingByDateSql = "$selectMaalingSql order by dato_start desc"

    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val selectMaalingByStatus = "$selectMaalingSql where status in (:statusList)"

    val updateMaalingSql = """update "testlab2_testing"."maalingv1" set navn = :navn, status = :status where id = :id"""

    val insertMaalingTestregelQuery =
        """insert into "testlab2_testing"."maaling_testregel" (maaling_id, testregel_id) values (:maaling_id, :testregel_id)"""

    val insertMaalingLoeysingQuery =
        """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"""

    fun updateMaalingParams(maaling: Maaling): Map<String, Any> {
      val status =
          when (maaling) {
            is Planlegging -> "planlegging"
            is Crawling -> "crawling"
            is Kvalitetssikring -> "kvalitetssikring"
            is Testing -> "testing"
            is TestingFerdig -> "testing_ferdig"
          }
      return mapOf("navn" to maaling.navn, "status" to status, "id" to maaling.id)
    }

    val deleteMaalingSql = """delete from "testlab2_testing"."maalingv1" where id = :id"""
  }

  @Transactional
  fun createMaaling(
      navn: String,
      datoStart: Instant,
      utval: Utval,
      testregelIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Int {
    val idMaaling =
        jdbcTemplate.queryForObject(
            createMaalingSql,
            createMaalingParams(navn, datoStart, crawlParameters, utval.id),
            Int::class.java)!!
    val loeysingIdList = utval.loeysingar.map { it.id }
      updateLoeysingarForMaaling(loeysingIdList, idMaaling)
      updateTestreglarForMaaling(testregelIdList, idMaaling)

    return idMaaling
  }

  @Transactional
  fun createMaaling(
      navn: String,
      datoStart: Instant,
      loyesingIds: List<Int>,
      testregelIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Int {
    val idMaaling =
        jdbcTemplate.update(
            createMaalingSql,
            createMaalingParams(navn, datoStart, crawlParameters))!!
      updateLoeysingarForMaaling(loyesingIds, idMaaling)
      updateTestreglarForMaaling(testregelIdList, idMaaling)

    return idMaaling
  }

    private fun updateTestreglarForMaaling(testregelIdList: List<Int>, idMaaling: Int) {
        for (idTestregel: Int in testregelIdList) {
            jdbcTemplate.update(
                insertMaalingTestregelQuery,
                mapOf("maaling_id" to idMaaling, "testregel_id" to idTestregel)
            )
        }
    }

    private fun updateLoeysingarForMaaling(loyesingIds: List<Int>, idMaaling: Int) {
        for (idLoeysing: Int in loyesingIds) {
            jdbcTemplate.update(
                insertMaalingLoeysingQuery, mapOf("idMaaling" to idMaaling, "idLoeysing" to idLoeysing)
            )
        }
    }

    @Cacheable("maalingCache", key = "#id")
  fun getMaaling(id: Int): Maaling {
    val maaling =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(selectMaalingByIdSql, mapOf("id" to id), maalingRowmapper))

    return maaling?.toMaaling() ?: throw NoSuchElementException("Fant ikke måling med id $id")
  }

  @Transactional
  @CacheEvict("maalingCache", key = "#id")
  fun deleteMaaling(id: Int): Int = jdbcTemplate.update(deleteMaalingSql, mapOf("id" to id))

  fun getMaalingList(): List<MaalingListElement> =
      jdbcTemplate
          .query(selectMaalingByDateSql, maalingRowmapper)
          .map { MaalingListElement(it.id, it.navn, it.datoStart, it.status.status) }
          .also { logger.debug("hentet ${it.size} målinger fra databasen") }

  fun getMaalingListByStatus(statusList: List<MaalingStatus>): List<Maaling> {
    return jdbcTemplate
        .query(
            selectMaalingByStatus,
            mapOf("statusList" to statusList.map { it.status }),
            maalingRowmapper)
        .map { it.toMaaling() }
        .also {
          logger.debug(
              "hentet ${it.size} målinger fra databasen med status 'crawling' eller 'testing'")
        }
  }

  private fun MaalingDTO.toMaaling(): Maaling {
      val loeysingList = getLoeysingList()
      return when (status) {
      planlegging -> {
          val testregelList =
              getTestregelList()
          Planlegging(
            id, navn, datoStart, loeysingList, testregelList, CrawlParameters(maxLenker, talLenker))
      }
      crawling,
      kvalitetssikring -> {
        val crawlResultat = getCrawlResultatForMaaling(loeysingList)
        if (status == crawling) {
          Crawling(this.id, this.navn, this.datoStart, crawlResultat)
        } else {
          Kvalitetssikring(id, navn, datoStart, crawlResultat)
        }
      }
      testing,
      testing_ferdig -> {
        val testKoeyringar = getTestKoeyringarForMaaling(id, loeysingList)
        if (status == testing) {
          Testing(id, navn, datoStart, testKoeyringar)
        } else {
          TestingFerdig(id, navn, datoStart, testKoeyringar)
        }
      }
    }
  }

    private fun MaalingDTO.getCrawlResultatForMaaling(loeysingList: List<Loeysing>) =
        crawlResultatForMaaling(id, loeysingList)

    private fun MaalingDTO.getTestregelList(): List<TestregelBase> {
        val testregelList =
            jdbcTemplate.query(maalingTestregelSql, mapOf("id" to id), testregelRowMapper).map {
                it.toTestregelBase()
            }
        return testregelList
    }

    private fun MaalingDTO.getLoeysingList(): List<Loeysing> {
        val query = """select idloeysing from testlab2_testing."maalingloeysing" where idmaaling = :id"""
        val loeysingIdList: List<Int> =
            jdbcTemplate.queryForList(
                query,
                mapOf("id" to id),
                Int::class.java
            )
        val loeysingList = loeysingsRegisterClient.getMany(loeysingIdList, datoStart).getOrThrow()
        return loeysingList
    }

    fun getCrawlParameters(maalingId: Int): CrawlParameters {
      val query = """select m.max_lenker, m.tal_lenker from testlab2_testing."maalingv1" m where m.id = :id"""
      return runCatching {
          jdbcTemplate.queryForObject(
              query,
              mapOf("id" to maalingId),
              crawlParametersRowmapper)
              ?: throw RuntimeException("Fant ikke crawlparametere for maaling $maalingId")
      }
          .getOrElse {
              logger.error(
                  "Kunne ikke hente crawlparametere for maaling $maalingId, velger default parametere")
              throw it
          }
  }

  private fun getTestKoeyringarForMaaling(
      maalingId: Int,
      loeysingList: List<Loeysing>
  ): List<TestKoeyring> {
    val crawlResultat = crawlResultatForMaaling(maalingId, loeysingList)
    return jdbcTemplate.query<TestKoeyring>(
        """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side,url_agg_side_tr,url_agg_loeysing, brukar_id
              from "testlab2_testing"."testkoeyring" t
              where maaling_id = :maaling_id
            """
            .trimIndent(),
        mapOf("maaling_id" to maalingId),
        fun(rs: ResultSet, _: Int): TestKoeyring {
          val status = rs.getString("status")
          val loeysingId = rs.getInt("loeysing_id")
            val crawlResultatForLoeysing =
                getCrawlresultatForLoeysing(crawlResultat, loeysingId)
            val brukar = getBrukarFromResultSet(rs)
          if (crawlResultatForLoeysing !is CrawlResultat.Ferdig) {
            throw RuntimeException(
                "crawlresultat for loeysing med id = $loeysingId er ikkje ferdig")
          }

          val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
          return when (status) {
            "ikkje_starta" -> {
                ikkjeStarta(crawlResultatForLoeysing, sistOppdatert, rs, brukar)
            }
            "starta" -> {
                starta(crawlResultatForLoeysing, sistOppdatert, rs, brukar)
            }
            "feila" ->
                feila(crawlResultatForLoeysing, sistOppdatert, rs, brukar)
            "ferdig" -> {
                ferdig(rs, crawlResultatForLoeysing, sistOppdatert, brukar)
            }
            else -> throw RuntimeException("ukjent status $status")
          }
        })
  }

    private fun ferdig(
        rs: ResultSet,
        crawlResultatForLoeysing: CrawlResultat.Ferdig,
        sistOppdatert: Instant,
        brukar: Brukar?
    ): TestKoeyring.Ferdig {
        val urlFulltResultat = rs.getString("url_fullt_resultat")
        val urlBrot = rs.getString("url_brot")
        val urlAggTR = rs.getString("url_agg_tr")
        val urlAggSK = rs.getString("url_agg_sk")
        val urlAggSide = rs.getString("url_agg_side")
        val urlAggSideTR = rs.getString("url_agg_side_tr")
        val urlAggLoeysing = rs.getString("url_agg_loeysing")

        val lenker =
            autoTesterLenker(
                urlFulltResultat,
                urlBrot,
                urlAggTR,
                urlAggSK,
                urlAggSide,
                urlAggSideTR,
                urlAggLoeysing
            )
        return TestKoeyring.Ferdig(
            crawlResultatForLoeysing,
            sistOppdatert,
            URI(rs.getString("status_url")).toURL(),
            lenker,
            brukar
        )
    }

    private fun autoTesterLenker(
        urlFulltResultat: String?,
        urlBrot: String?,
        urlAggTR: String?,
        urlAggSK: String?,
        urlAggSide: String?,
        urlAggSideTR: String?,
        urlAggLoeysing: String?
    ): AutoTesterClient.AutoTesterLenker? {
        val lenker =
            if (urlFulltResultat != null)
                AutoTesterClient.AutoTesterLenker(
                    URI(urlFulltResultat).toURL(),
                    URI(urlBrot).toURL(),
                    URI(urlAggTR).toURL(),
                    URI(urlAggSK).toURL(),
                    URI(urlAggSide).toURL(),
                    URI(urlAggSideTR).toURL(),
                    URI(urlAggLoeysing).toURL()
                )
            else null
        return lenker
    }

    private fun feila(
        crawlResultatForLoeysing: CrawlResultat.Ferdig,
        sistOppdatert: Instant,
        rs: ResultSet,
        brukar: Brukar?
    ) = TestKoeyring.Feila(
        crawlResultatForLoeysing, sistOppdatert, rs.getString("feilmelding"), brukar
    )

    private fun starta(
        crawlResultatForLoeysing: CrawlResultat.Ferdig,
        sistOppdatert: Instant,
        rs: ResultSet,
        brukar: Brukar?
    ) = TestKoeyring.Starta(
        crawlResultatForLoeysing,
        sistOppdatert,
        URI(rs.getString("status_url")).toURL(),
        Framgang(rs.getInt("lenker_testa"), crawlResultatForLoeysing.antallNettsider),
        brukar
    )

    private fun ikkjeStarta(
        crawlResultatForLoeysing: CrawlResultat.Ferdig,
        sistOppdatert: Instant,
        rs: ResultSet,
        brukar: Brukar?
    ) = TestKoeyring.IkkjeStarta(
        crawlResultatForLoeysing,
        sistOppdatert,
        URI(rs.getString("status_url")).toURL(),
        brukar
    )

    private fun getBrukarFromResultSet(rs: ResultSet) = brukarService.getBrukarById(rs.getInt("brukar_id"))

    private fun getCrawlresultatForLoeysing(
        crawlResultat: List<CrawlResultat>,
        loeysingId: Int
    ): CrawlResultat {
        val crawlResultatForLoeysing =
            crawlResultat.find { it.loeysing.id == loeysingId }
                ?: throw RuntimeException(
                    "finner ikkje crawlresultat for loeysing med id = $loeysingId"
                )
        return crawlResultatForLoeysing
    }

    private fun crawlResultatForMaaling(
        maalingId: Int,
        loeysingList: List<Loeysing>
    ) = sideutvalDAO.getCrawlResultatForMaaling(maalingId, loeysingList)

    @Transactional
  fun updateMaaling(maaling: Maaling) {
    val cache = cacheManager.getCache("maalingCache")

    if (cache != null) {
      cache.evict(maaling.id)
    } else {
      logger.warn("Finner ikkje maalingCache")
    }

    if (maaling is Planlegging) {
        updateMaaling(maaling)
        val deleteParams = deleteFromMaalingLoeysing(maaling)
        updateMaalingTestregel(maaling)
        deleteFromMaalingTestregel(deleteParams)
        insertMaalingTestregel(maaling)
    } else {
      jdbcTemplate.update(updateMaalingSql, updateMaalingParams(maaling))
    }
  }

    private fun insertMaalingTestregel(maaling: Planlegging) {
        val updateBatchValuesTestregel =
            maaling.testregelList.map { mapOf("maaling_id" to maaling.id, "testregel_id" to it.id) }
        jdbcTemplate.batchUpdate(
            insertMaalingTestregelQuery, updateBatchValuesTestregel.toTypedArray()
        )
    }

    private fun deleteFromMaalingTestregel(deleteParams: Map<String, Int>) {
        jdbcTemplate.update(
            """delete from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""", deleteParams
        )
    }

    private fun updateMaalingTestregel(maaling: Planlegging) {
        val updateMaalingTestregelLoeysingQuery = """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:maalingId, :loeysingId)"""

        val updateBatchValuesLoeysing =
            maaling.loeysingList.map { mapOf("maalingId" to maaling.id, "loeysingId" to it.id) }
        jdbcTemplate.batchUpdate(
            updateMaalingTestregelLoeysingQuery,
            updateBatchValuesLoeysing.toTypedArray()
        )
    }

    private fun deleteFromMaalingLoeysing(maaling: Maaling): Map<String, Int> {
        val deleteParams = mapOf("maalingId" to maaling.id)
        val deleteMaalingLoeysingQuery =
            """delete from "testlab2_testing"."maalingloeysing" where idMaaling = :maalingId"""

        jdbcTemplate.update(deleteMaalingLoeysingQuery, deleteParams)
        return deleteParams
    }

    private fun updateMaaling(maaling: Planlegging) {
        val updateQuery =
            """update "testlab2_testing"."maalingv1" set navn = :navn, status = :status, max_lenker = :max_lenker, tal_lenker = :tal_lenker where id = :id"""


        jdbcTemplate.update(
            updateQuery,
            mapOf(
                "id" to maaling.id,
                "navn" to maaling.navn,
                "status" to "planlegging",
                "max_lenker" to maaling.crawlParameters.maxLenker,
                "tal_lenker" to maaling.crawlParameters.talLenker
            )
        )
    }

    @Transactional
  fun save(maaling: Maaling): Result<Maaling> = runCatching {
    updateMaaling(maaling)
    when (maaling) {
      is Planlegging -> {}
      is Crawling -> {
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Kvalitetssikring -> {
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Testing -> {
        maaling.testKoeyringar.forEach { saveTestKoeyring(it, maaling.id) }
      }
      is TestingFerdig -> {
        maaling.testKoeyringar.forEach { saveTestKoeyring(it, maaling.id) }
      }
    }
    maaling
  }

  @Transactional
  fun saveMany(maalinger: Collection<Maaling>): Result<Collection<Maaling>> = runCatching {
    maalinger.forEach { save(it) }
    maalinger
  }

  @Transactional
  fun saveTestKoeyring(testKoeyring: TestKoeyring, maalingId: Int) {
      deleteExistingTestkoeyring(maalingId, testKoeyring)
    when (testKoeyring) {
      is TestKoeyring.Starta -> {
        saveTestKoeyringStarta(maalingId, testKoeyring)
      }
      is TestKoeyring.Ferdig -> {
        saveTestKoeyringFerdig(maalingId, testKoeyring)
      }
      else -> {
          saveNyTestKoeyring(maalingId, testKoeyring)
      }
    }
  }

    private fun saveNyTestKoeyring(
        maalingId: Int,
        testKoeyring: TestKoeyring
    ) {
        jdbcTemplate.queryForObject(
            """insert into "testlab2_testing"."testkoeyring" (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, brukar_id) 
                    values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding, :brukar_id)
                    returning id
                """
                .trimMargin(),
            mapOf(
                "maaling_id" to maalingId,
                "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
                "status" to status(testKoeyring),
                "status_url" to statusURL(testKoeyring),
                "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
                "feilmelding" to feilmelding(testKoeyring),
                "brukar_id" to getBrukar(testKoeyring.brukar)
            ),
            Int::class.java
        )
    }

    private fun deleteExistingTestkoeyring(
        maalingId: Int,
        testKoeyring: TestKoeyring
    ) {
        jdbcTemplate.update(
            """delete from "testlab2_testing"."testkoeyring" where maaling_id = :maaling_id and loeysing_id = :loeysing_id""",
            mapOf("maaling_id" to maalingId, "loeysing_id" to testKoeyring.crawlResultat.loeysing.id)
        )
    }

    private fun getBrukar(brukar: Brukar?): Int? {
    if (brukar != null) {
      return brukarService.getBrukarIdByBrukarnamn(brukar.brukarnamn)
    }
    return null
  }

  private fun saveTestKoeyringFerdig(maalingId: Int, testKoeyring: TestKoeyring.Ferdig) {
    jdbcTemplate.queryForObject(
        """
                  insert into "testlab2_testing"."testkoeyring"(maaling_id, loeysing_id, status, status_url, sist_oppdatert, url_fullt_resultat, url_brot, url_agg_tr, url_agg_sk,url_agg_side, url_agg_side_tr, url_agg_loeysing,brukar_id)
                  values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :url_fullt_resultat, :url_brot, :url_agg_tr, :url_agg_sk, :url_agg_side,:url_agg_side_tr,:url_agg_loeysing,:brukar_id)
                  returning id
                """
            .trimIndent(),
        mapOf(
            "maaling_id" to maalingId,
            "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
            "status" to status(testKoeyring),
            "status_url" to statusURL(testKoeyring),
            "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
            "url_fullt_resultat" to testKoeyring.lenker?.urlFulltResultat?.toString(),
            "url_brot" to testKoeyring.lenker?.urlBrot?.toString(),
            "url_agg_tr" to testKoeyring.lenker?.urlAggregeringTR?.toString(),
            "url_agg_sk" to testKoeyring.lenker?.urlAggregeringSK?.toString(),
            "url_agg_side" to testKoeyring.lenker?.urlAggregeringSide?.toString(),
            "url_agg_side_tr" to testKoeyring.lenker?.urlAggregeringSideTR?.toString(),
            "url_agg_loeysing" to testKoeyring.lenker?.urlAggregeringLoeysing?.toString(),
            "brukar_id" to getBrukar(testKoeyring.brukar)),
        Int::class.java)
  }

  private fun saveTestKoeyringStarta(maalingId: Int, testKoeyring: TestKoeyring.Starta) {
    jdbcTemplate.queryForObject(
        """insert into "testlab2_testing"."testkoeyring" (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, lenker_testa, brukar_id) 
                    values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding, :lenker_testa,:brukar_id)
                    returning id
                """
            .trimMargin(),
        mapOf(
            "maaling_id" to maalingId,
            "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
            "status" to status(testKoeyring),
            "status_url" to statusURL(testKoeyring),
            "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
            "feilmelding" to feilmelding(testKoeyring),
            "lenker_testa" to testKoeyring.framgang.prosessert,
            "brukar_id" to getBrukar(testKoeyring.brukar)),
        Int::class.java)
  }

  @Transactional
  fun updateKontrollId(kontrollId: Int, maalingId: Int) =
      jdbcTemplate.update(
          """update "testlab2_testing"."maalingv1" set kontrollId = :kontrollId where id = :maalingId""",
          mapOf("kontrollId" to kontrollId, "maalingId" to maalingId))

  fun getMaalingIdFromKontrollId(kontrollId: Int): Int? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              """select id from testlab2_testing."maalingv1" where kontrollId = :kontrollId""",
              mapOf("kontrollId" to kontrollId),
          ) { rs, _ ->
            rs.getInt("id")
          })

  private fun status(testKoeyring: TestKoeyring): String =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> "ikkje_starta"
        is TestKoeyring.Starta -> "starta"
        is TestKoeyring.Feila -> "feila"
        is TestKoeyring.Ferdig -> "ferdig"
      }

  private fun feilmelding(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.Feila -> testKoeyring.feilmelding
        else -> null
      }

  private fun statusURL(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Starta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Ferdig -> testKoeyring.statusURL.toString()
        else -> null
      }

  fun getBrukarForMaaling(maalingId: Int): List<String> {
    return jdbcTemplate.queryForList(
        """
                select distinct b.namn as namn
                from "testlab2_testing"."testkoeyring" ti
                join "testlab2_testing"."brukar" b on ti.brukar_id = b.id
                where ti.maaling_id = :maaling_id
            """
            .trimIndent(),
        mapOf("maaling_id" to maalingId),
        String::class.java)
  }

  fun getMaalingForKontroll(kontrollId: Int): Result<Int> {
    return runCatching {
      jdbcTemplate.queryForObject(
          """
                select id
                from "testlab2_testing"."maalingv1"
                where kontrollid = :kontrollId
            """
              .trimIndent(),
          mapOf("kontrollId" to kontrollId),
          Int::class.java)
          ?: throw NoSuchElementException("Fant ikke måling for kontroll $kontrollId")
    }
  }
}
