package no.uutilsynet.testlab2testing.forenkletkontroll

import java.sql.Timestamp
import java.time.Instant
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
import no.uutilsynet.testlab2testing.loeysing.utval.Utval
import no.uutilsynet.testlab2testing.loeysing.utval.UtvalId
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MaalingDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val sideutvalDAO: SideutvalDAO,
    val cacheManager: CacheManager,
    val testkoeyringDAO: TestkoeyringDAO,
    val maalingMapper: MaalingMapper
) {

  private val logger = LoggerFactory.getLogger(MaalingDAO::class.java)

  object MaalingParams {
    val maalingRowmapper =
        RowMapper<MaalingDbRow> { rs, _ ->
          MaalingDbRow(
              id = rs.getInt("id"),
              navn = rs.getString("navn"),
              datoStart = rs.getTimestamp("dato_start").toInstant(),
              status = MaalingStatus.valueOf(rs.getString("status")),
              maxLenker = rs.getInt("max_lenker"),
              talLenker = rs.getInt("tal_lenker"))
        }
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

    const val selectMaalingSql =
        """select id, navn, dato_start, status, max_lenker, tal_lenker from "testlab2_testing"."maalingv1""""

    val selectMaalingByDateSql = "$selectMaalingSql order by dato_start desc"

    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val selectMaalingByStatus = "$selectMaalingSql where status in (:statusList)"

    const val updateMaalingSql =
        """update "testlab2_testing"."maalingv1" set navn = :navn, status = :status where id = :id"""

    const val insertMaalingTestregelQuery =
        """insert into "testlab2_testing"."maaling_testregel" (maaling_id, testregel_id) 
            values (:maaling_id, :testregel_id)"""

    const val insertMaalingLoeysingQuery =
        """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"""

    fun updateMaalingParams(maaling: Maaling): Map<String, Any> {
      val status =
          when (maaling) {
            is Maaling.Planlegging -> "planlegging"
            is Maaling.Crawling -> "crawling"
            is Maaling.Kvalitetssikring -> "kvalitetssikring"
            is Maaling.Testing -> "testing"
            is Maaling.TestingFerdig -> "testing_ferdig"
          }
      return mapOf("navn" to maaling.navn, "status" to status, "id" to maaling.id)
    }

    const val deleteMaalingSql = """delete from "testlab2_testing"."maalingv1" where id = :id"""
  }

  fun getMaalingList(): List<MaalingListElement> =
      jdbcTemplate
          .query(selectMaalingByDateSql, maalingRowmapper)
          .map { MaalingListElement(it.id, it.navn, it.datoStart, it.status.status) }
          .also { logger.debug("hentet ${it.size} målinger fra databasen") }

  @Cacheable("maalingCache", key = "#id")
  fun getMaaling(id: Int): Maaling {
    val maaling: MaalingDbRow? =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(selectMaalingByIdSql, mapOf("id" to id), maalingRowmapper))

    return maaling?.let(maalingMapper::toMaaling)
        ?: throw NoSuchElementException("Fant ikke måling med id $id")
  }

  fun getMaalingListByStatus(statusList: List<MaalingStatus>): List<Maaling> {
    return jdbcTemplate
        .query(
            selectMaalingByStatus,
            mapOf("statusList" to statusList.map { it.status }),
            maalingRowmapper)
        .map(maalingMapper::toMaaling)
        .also {
          logger.debug(
              "hentet {} målinger fra databasen med status {}",
              it.size,
              statusList.map { status -> status.status })
        }
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
    insertLoeysingarForMaaling(loeysingIdList, idMaaling)
    insertTestreglarForMaaling(testregelIdList, idMaaling)

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

    insertLoeysingarForMaaling(loyesingIds, idMaaling)
    insertTestreglarForMaaling(testregelIdList, idMaaling)

    return idMaaling
  }

  @Transactional
  @CacheEvict("maalingCache", key = "#id")
  fun deleteMaaling(id: Int): Int = jdbcTemplate.update(deleteMaalingSql, mapOf("id" to id))

  @Transactional
  fun updateMaaling(maaling: Maaling) {
    val cache = cacheManager.getCache("maalingCache")

    if (cache != null) {
      cache.evict(maaling.id)
    } else {
      logger.warn("Finner ikkje maalingCache")
    }

    if (maaling is Maaling.Planlegging) {
      updatePlanlegging(maaling)
      deleteLoeysingarForMaaling(maaling.id)
      insertMaalingLoeysing(maaling)
      deleteTestreglarForMaaling(maaling.id)
      insertMaalingTestregel(maaling)
    } else {
      jdbcTemplate.update(updateMaalingSql, updateMaalingParams(maaling))
    }
  }

  @Transactional
  fun save(maaling: Maaling): Result<Maaling> = runCatching {
    updateMaaling(maaling)
    when (maaling) {
      is Maaling.Planlegging -> Unit
      is Maaling.Crawling -> {
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Maaling.Kvalitetssikring -> {
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Maaling.Testing -> {
        maaling.testKoeyringar.forEach { testkoeyringDAO.saveTestKoeyring(it, maaling.id) }
      }
      is Maaling.TestingFerdig -> {
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

  private fun insertTestreglarForMaaling(testregelIdList: List<Int>, maalingId: Int) {
    for (testregelId: Int in testregelIdList) {
      jdbcTemplate.update(
          insertMaalingTestregelQuery,
          mapOf("maaling_id" to maalingId, "testregel_id" to testregelId))
    }
  }

  private fun insertLoeysingarForMaaling(loeysingIds: List<Int>, maalingId: Int) {
    for (loeysingId: Int in loeysingIds) {
      jdbcTemplate.update(
          insertMaalingLoeysingQuery, mapOf("idMaaling" to maalingId, "idLoeysing" to loeysingId))
    }
  }

  private fun insertMaalingTestregel(maaling: Maaling.Planlegging) {
    val updateBatchValuesTestregel =
        maaling.testregelList.map { mapOf("maaling_id" to maaling.id, "testregel_id" to it.id) }
    jdbcTemplate.batchUpdate(insertMaalingTestregelQuery, updateBatchValuesTestregel.toTypedArray())
  }

  private fun deleteTestreglarForMaaling(maalingId: Int) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""",
        mapOf("maalingId" to maalingId))
  }

  private fun insertMaalingLoeysing(maaling: Maaling.Planlegging) {
    val updateMaalingLoeysingQuery =
        """insert into "testlab2_testing"."maalingloeysing" (idMaaling, idLoeysing) values (:maalingId, :loeysingId)"""

    val updateBatchValuesLoeysing =
        maaling.loeysingList.map { mapOf("maalingId" to maaling.id, "loeysingId" to it.id) }
    jdbcTemplate.batchUpdate(updateMaalingLoeysingQuery, updateBatchValuesLoeysing.toTypedArray())
  }

  private fun deleteLoeysingarForMaaling(maalingId: Int) {
    val deleteMaalingLoeysingQuery =
        """delete from "testlab2_testing"."maalingloeysing" where idMaaling = :maalingId"""

    jdbcTemplate.update(deleteMaalingLoeysingQuery, mapOf("maalingId" to maalingId))
  }

  private fun updatePlanlegging(maaling: Maaling.Planlegging) {
    val updateQuery =
        """update "testlab2_testing"."maalingv1" 
            set navn = :navn, status = :status, max_lenker = :max_lenker, tal_lenker = :tal_lenker 
            where id = :id"""
            .trimMargin()

    jdbcTemplate.update(
        updateQuery,
        mapOf(
            "id" to maaling.id,
            "navn" to maaling.navn,
            "status" to "planlegging",
            "max_lenker" to maaling.crawlParameters.maxLenker,
            "tal_lenker" to maaling.crawlParameters.talLenker))
  }
}
