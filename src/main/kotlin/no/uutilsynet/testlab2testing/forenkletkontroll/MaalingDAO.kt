package no.uutilsynet.testlab2testing.forenkletkontroll

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.*
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.crawlParametersRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.deleteMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.insertMaalingLoeysingQuery
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.insertMaalingTestregelQuery
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.maalingTestregelQuery
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
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.model.TestregelBase
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MaalingDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val sideutvalDAO: SideutvalDAO,
    val cacheManager: CacheManager,
    val testkoeyringDAO: TestkoeyringDAO,
    val testregelClient: TestregelClient
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
    ): MapSqlParameterSource {

      val params = MapSqlParameterSource()
      params.addValue("navn", navn)
      params.addValue("dato_start", Timestamp.from(datoStart))
      params.addValue("status", "planlegging")
      params.addValue("max_lenker", crawlParameters.maxLenker)
      params.addValue("tal_lenker", crawlParameters.talLenker)
      params.addValue("utvalId", utvalId)
      return params
    }

    val selectMaalingSql =
        """select id, navn, dato_start, status, max_lenker, tal_lenker from "testlab2_testing"."maalingv1""""

    val selectMaalingByDateSql = "$selectMaalingSql order by dato_start desc"

    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val selectMaalingByStatus = "$selectMaalingSql where status in (:statusList)"

    val updateMaalingSql =
        """update "testlab2_testing"."maalingv1" set navn = :navn, status = :status where id = :id"""

    val insertMaalingTestregelQuery =
        """insert into "testlab2_testing"."maaling_testregel" (maaling_id, testregel_id) values (:maaling_id, :testregel_id)"""

    val insertMaalingLoeysingQuery =
        """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"""

    val maalingTestregelQuery =
        """
         select testregel_id from testlab2_testing.maaling_testregel where maaling_id = :id
     """
            .trimIndent()

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
    val keyHolder: KeyHolder = GeneratedKeyHolder()
    jdbcTemplate.update(
        createMaalingSql,
        createMaalingParams(navn, datoStart, crawlParameters, utval.id),
        keyHolder)

    val idMaaling = keyHolder.keys?.get("id") as Int
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
    val keyHolder = GeneratedKeyHolder()

    jdbcTemplate.update(
        createMaalingSql, createMaalingParams(navn, datoStart, crawlParameters), keyHolder)
    val idMaaling = keyHolder.keys?.get("id") as Int

    updateLoeysingarForMaaling(loyesingIds, idMaaling)
    updateTestreglarForMaaling(testregelIdList, idMaaling)

    return idMaaling
  }

  private fun updateTestreglarForMaaling(testregelIdList: List<Int>, idMaaling: Int) {
    for (idTestregel: Int in testregelIdList) {
      jdbcTemplate.update(
          insertMaalingTestregelQuery,
          mapOf("maaling_id" to idMaaling, "testregel_id" to idTestregel))
    }
  }

  private fun updateLoeysingarForMaaling(loyesingIds: List<Int>, idMaaling: Int) {
    for (idLoeysing: Int in loyesingIds) {
      jdbcTemplate.update(
          insertMaalingLoeysingQuery, mapOf("idMaaling" to idMaaling, "idLoeysing" to idLoeysing))
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
    return when (status) {
      planlegging -> {
        Planlegging(
            id,
            navn,
            datoStart,
            getLoeysingarForMaaling(id, datoStart),
            getTestregelList(),
            CrawlParameters(maxLenker, talLenker))
      }
      crawling -> {
        Crawling(id, navn, datoStart, getCrawlResultatForMaaling())
      }
      kvalitetssikring -> {
        Kvalitetssikring(id, navn, datoStart, getCrawlResultatForMaaling())
      }
      testing -> {
        Testing(id, navn, datoStart, getTestkoeyingarForMaaling())
      }
      testing_ferdig -> {
        TestingFerdig(id, navn, datoStart, getTestkoeyingarForMaaling())
      }
    }
  }

  private fun MaalingDTO.getTestkoeyingarForMaaling() =
      testkoeyringDAO.getTestKoeyringarForMaaling(
          id, loeysingsMetadataForMaaling(id, getLoeysingarForMaaling(id, datoStart)))

  private fun MaalingDTO.getCrawlResultatForMaaling() =
      crawlResultatForMaaling(id, getLoeysingarForMaaling(id, datoStart))

  private fun MaalingDTO.getTestregelList(): List<TestregelBase> {
    val testregelIds =
        getTestrelIdForMaaling(id)

    return testregelClient.getTestregelListFromIds(testregelIds).getOrThrow().map { it.toTestregelBase() }
  }

  fun getLoeysingarForMaaling(id: Int, datoStart: Instant): List<Loeysing> {
    val query =
        """select idloeysing from "testlab2_testing"."maalingloeysing" where idmaaling = :id"""
    val loeysingIdList: List<Int> =
        jdbcTemplate.queryForList(query, mapOf("id" to id), Int::class.java)
    val loeysingList =
        loeysingsRegisterClient
            .getMany(loeysingIdList, datoStart)
            .fold(
                onSuccess = { it },
                onFailure = {
                  logger.error("Feil ved henting av løysingar $loeysingIdList for maaling $id", it)
                  throw it
                })
    return loeysingList
  }

  fun getLoeysingarForMaaling(id: Int): List<Loeysing> {
    return getLoeysingarForMaaling(id, Instant.now())
  }

  fun getCrawlParameters(maalingId: Int): CrawlParameters {
    val query =
        """select m.max_lenker, m.tal_lenker from testlab2_testing."maalingv1" m where m.id = :id"""
    return runCatching {
          jdbcTemplate.queryForObject(query, mapOf("id" to maalingId), crawlParametersRowmapper)
              ?: throw RuntimeException("Fant ikke crawlparametere for maaling $maalingId")
        }
        .getOrElse {
          logger.error(
              "Kunne ikke hente crawlparametere for maaling $maalingId, velger default parametere")
          throw it
        }
  }

  private fun crawlResultatForMaaling(maalingId: Int, loeysingList: List<Loeysing>) =
      sideutvalDAO.getCrawlResultatForMaaling(maalingId, loeysingList)

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
      deleteFromMaalingLoeysing(maaling)
      updateMaalingTestregel(maaling)
      deleteFromMaalingTestregel(maaling.id)
      insertMaalingTestregel(maaling)
    } else {
      jdbcTemplate.update(updateMaalingSql, updateMaalingParams(maaling))
    }
  }

  private fun insertMaalingTestregel(maaling: Planlegging) {
    val updateBatchValuesTestregel =
        maaling.testregelList.map { mapOf("maaling_id" to maaling.id, "testregel_id" to it.id) }
    jdbcTemplate.batchUpdate(insertMaalingTestregelQuery, updateBatchValuesTestregel.toTypedArray())
  }

  private fun deleteFromMaalingTestregel(maalingId: Int) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""",
        mapOf("maalingId" to maalingId))
  }

  private fun updateMaalingTestregel(maaling: Planlegging) {
    val updateMaalingTestregelLoeysingQuery =
        """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:maalingId, :loeysingId)"""

    val updateBatchValuesLoeysing =
        maaling.loeysingList.map { mapOf("maalingId" to maaling.id, "loeysingId" to it.id) }
    jdbcTemplate.batchUpdate(
        updateMaalingTestregelLoeysingQuery, updateBatchValuesLoeysing.toTypedArray())
  }

  private fun deleteFromMaalingLoeysing(maaling: Maaling) {
    val deleteMaalingLoeysingQuery =
        """delete from "testlab2_testing"."maalingloeysing" where idMaaling = :maalingId"""

    jdbcTemplate.update(deleteMaalingLoeysingQuery, mapOf("maalingId" to maaling.id))
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
            "tal_lenker" to maaling.crawlParameters.talLenker))
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
        maaling.testKoeyringar.forEach { testkoeyringDAO.saveTestKoeyring(it, maaling.id) }
      }
      is TestingFerdig -> {
        maaling.testKoeyringar.forEach { testkoeyringDAO.saveTestKoeyring(it, maaling.id) }
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
  fun updateKontrollId(kontrollId: Int, maalingId: Int) =
      jdbcTemplate.update(
          """update "testlab2_testing"."maalingv1" set kontrollId = :kontrollId where id = :maalingId""",
          mapOf("kontrollId" to kontrollId, "maalingId" to maalingId))

  fun getMaalingIdFromKontrollId(kontrollId: Int): Int? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              """select id from "testlab2_testing"."maalingv1" where kontrollId = :kontrollId""",
              mapOf("kontrollId" to kontrollId),
          ) { rs, _ ->
            rs.getInt("id")
          })

  fun getTestrelIdForMaaling(maalingId: Int): List<Int> {
    return jdbcTemplate.queryForList(
        """select testregel_id from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""",
        mapOf("maalingId" to maalingId),
        Int::class.java)
  }

  fun getMaalingIdForTestregel(testregelId: Int): Result<List<Int>> {
    return runCatching {
      jdbcTemplate.queryForList(
          """select maaling_id from "testlab2_testing"."maaling_testregel" where testregel_id = :testregelId""",
          mapOf("testregelId" to testregelId),
          Int::class.java)
    }
  }

  private fun loeysingsMetadataForMaaling(
      maalingId: Int,
      loeysingList: List<Loeysing>
  ): Map<Int, LoeysingMetadata> {
    return sideutvalDAO
        .getCrawlResultatForMaaling(maalingId, loeysingList)
        .filterIsInstance<CrawlResultat.Ferdig>()
        .associate {
          it.loeysing.id to LoeysingMetadata(it.loeysing.id, it.loeysing, it.antallNettsider)
        }
  }

  fun hasMaalingTestregel(testregelId: Int): Boolean {
    val sql =
        "SELECT COUNT(*) FROM testlab2_testing.maaling_testregel WHERE testregel_id = :testregelId"
    val params =
        MapSqlParameterSource()
            .addValue(
                "testregelId",
                testregelId,
            )
    val count = jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0
    return count > 0
  }

  data class LoeysingMetadata(val id: Int, val loeysing: Loeysing, val antallNettsider: Int)
}
