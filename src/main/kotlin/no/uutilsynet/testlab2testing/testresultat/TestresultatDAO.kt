package no.uutilsynet.testlab2testing.testresultat

import io.micrometer.observation.annotation.Observed
import java.sql.ResultSet
import java.sql.Timestamp
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.common.SortParamTestregel
import no.uutilsynet.testlab2testing.testresultat.model.TestresultatExport
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class TestresultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  val logger = LoggerFactory.getLogger(this::class.java)

  private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
    TestresultatDB(
        id = rs.getInt("id"),
        testgrunnlagId = rs.getInt("testgrunnlag_id"),
        maalingId = rs.getInt("maaling_id"),
        testregelId = rs.getInt("testregel_id"),
        loeysingId = rs.getInt("loeysing_id"),
        sideutvalId = rs.getInt("crawl_side_id"),
        side = rs.getString("url"),
        testUtfoert = rs.getTimestamp("test_vart_utfoert").toInstant(),
        elementUtfall = rs.getString("element_utfall"),
        elementResultat = TestresultatUtfall.valueOf(rs.getString("element_resultat")),
        elementOmtalePointer = rs.getString("element_omtale_pointer"),
        elmentOmtaleHtml = rs.getString("element_omtale_html"),
        elementOmtaleDescription = rs.getString("element_omtale"),
        brukarId = rs.getInt("brukar_id"))
  }

  fun create(testresultat: TestresultatDBBase): Int {
    val sql =
        """
            INSERT INTO testresultat (
            testgrunnlag_id, maaling_id,
                testregel_id, loeysing_id, crawl_side_id, test_vart_utfoert, element_utfall, element_resultat,
                element_omtale_pointer, element_omtale_html, element_omtale, brukar_id
            ) VALUES (
            :testgrunnlagId, :maalingId,
                :testregelId, :loeysingId, :sideutvalId, :testUtfoert, :elementUtfall, :elementResultat,
                :elementOmtalePointer, :elmentOmtalerHtml, :elementOmtalerDescription, :brukarid
            )
            RETURNING id
        """
            .trimIndent()
    val params =
        MapSqlParameterSource()
            .addValue("testgrunnlagId", testresultat.testgrunnlagId)
            .addValue("maalingId", testresultat.maalingId)
            .addValue("testregelId", testresultat.testregelId)
            .addValue("loeysingId", testresultat.loeysingId)
            .addValue("sideutvalId", testresultat.sideutvalId)
            .addValue("testUtfoert", Timestamp.from(testresultat.testUtfoert))
            .addValue("elementUtfall", testresultat.elementUtfall)
            .addValue("elementResultat", testresultat.elementResultat.name)
            .addValue("elementOmtalePointer", testresultat.elementOmtalePointer)
            .addValue("elmentOmtalerHtml", testresultat.elmentOmtaleHtml)
            .addValue("elementOmtalerDescription", testresultat.elementOmtaleDescription)
            .addValue("brukarid", testresultat.brukarId)
    return jdbcTemplate.queryForObject(sql, params, Int::class.java)!!
  }

  fun read(id: Int): TestresultatDB? {
    val sql = "SELECT * FROM testresultat WHERE id = :id"
    val params = MapSqlParameterSource().addValue("id", id)
    return jdbcTemplate.query(sql, params, rowMapper).firstOrNull()
  }

  fun update(testresultat: TestresultatDB): Int {
    val sql =
        """
            UPDATE testresultat
            SET
             testgrunnlag_id = :testgrunnlagId,
             maaling_id = :maalingId,
             testregel_id = :testregelId,
                loeysing_id = :loeysingId,
                sideutval_id = :sideutvalId,
                test_vart_utfoert = :testUtfoert,
                element_utfall = :elementUtfall,
                element_resultat = :elementResultat,
                element_omtale_pointer = :elementOmtalePointer,
                element_omtale_html = :elmentOmtaleHtml,
                element_omtale = :elementOmtaleDescription,
                brukar_id = :brukarid
            WHERE id = :id
        """
            .trimIndent()
    val params =
        MapSqlParameterSource()
            .addValue("id", testresultat.id)
            .addValue("testgrunnlagId", testresultat.testgrunnlagId)
            .addValue("maalingId", testresultat.maalingId)
            .addValue("testregelId", testresultat.testregelId)
            .addValue("loeysingId", testresultat.loeysingId)
            .addValue("sideutvalId", testresultat.sideutvalId)
            .addValue("testUtfoert", Timestamp.from(testresultat.testUtfoert))
            .addValue("elementUtfall", testresultat.elementUtfall)
            .addValue("elementResultat", testresultat.elementResultat.name)
            .addValue("elementOmtalePointer", testresultat.elementOmtalePointer)
            .addValue("elmentOmtalerHtml", testresultat.elmentOmtaleHtml)
            .addValue("elementOmtalerDescription", testresultat.elementOmtaleDescription)
            .addValue("brukarid", testresultat.brukarId)
    return jdbcTemplate.update(sql, params)
  }

  fun delete(id: Int): Int {
    val sql = "DELETE FROM testresultat WHERE id = :id"
    val params = MapSqlParameterSource().addValue("id", id)
    return jdbcTemplate.update(sql, params)
  }

  @Observed(name = "List<TestresultatDB> listBy maalingId and loeysingId brot")
  fun listBy(maalingId: Int, loeysingId: Int): List<TestresultatDB> {
    val sql =
        "SELECT * FROM testresultat t LEFT JOIN crawl_side cs ON t.crawl_side_id=cs.id WHERE maaling_id = :maalingId and loeysing_id= :loeysingId and element_resultat= 'brot'"
    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
    return runCatching { jdbcTemplate.query(sql, params, rowMapper) }
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error(it.message)
              emptyList()
            })
  }

  @Observed(name = "List<TestresultatDB> listBy maalingId and loeysingId brot")
  fun listBy(maalingId: Int): List<TestresultatDB> {
    val sql =
        "SELECT * FROM testresultat t LEFT JOIN crawl_side cs ON t.crawl_side_id=cs.id WHERE maaling_id = :maalingId"
    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
    return runCatching { jdbcTemplate.query(sql, params, rowMapper) }
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error(it.message)
              emptyList()
            })
  }

  @Observed(name = "List<TestresultatDB> listBy maalingId and loeysingId brot")
  fun listBy(
      maalingId: Int,
      loeysingId: Int?,
      testregelId: Int,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDB> {
    val sql =
        "SELECT * FROM testresultat t LEFT JOIN crawl_side cs ON t.crawl_side_id=cs.id WHERE maaling_id = :maalingId and loeysing_id= :loeysingId and testregel_Id=:testregelId and element_resultat= 'brot' order by %s %s limit :limit offset :offset"

    val formated =
        sql.format(
            mapSortParamToDBField(sortPaginationParams.sortParam),
            sortPaginationParams.sortOrder.name)

    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
            .addValue("testregelId", testregelId)
            .addValue("limit", sortPaginationParams.pageSize)
            .addValue("offset", sortPaginationParams.pageNumber * sortPaginationParams.pageSize)

    return runCatching { jdbcTemplate.query(formated, params, rowMapper) }
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error(it.message)
              emptyList()
            })
  }

  fun listBy(
      maalingId: Int,
      loeysingId: Int?,
      testregelIds: List<Int>,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDB> {
    val sql =
        "SELECT * FROM testresultat t LEFT JOIN crawl_side cs ON t.crawl_side_id=cs.id WHERE maaling_id = :maalingId and loeysing_id= :loeysingId and testregel_Id in (:testregelIds) and element_resultat= 'brot' order by %s %s limit :limit offset :offset"

    val formated =
        sql.format(
            mapSortParamToDBField(sortPaginationParams.sortParam),
            sortPaginationParams.sortOrder.name)

    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
            .addValue("testregelIds", testregelIds)
            .addValue("limit", sortPaginationParams.pageSize)
            .addValue("offset", sortPaginationParams.pageNumber * sortPaginationParams.pageSize)

    return jdbcTemplate.query(formated, params, rowMapper)
  }

  fun hasResultInDB(maalingId: Int, loeysingId: Int): Boolean {
    val sql =
        "SELECT COUNT(*) FROM testresultat WHERE maaling_id = :maalingId and loeysing_id= :loeysingId"
    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
    val count = jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0
    return count > 0
  }

  fun getTalBrotForKontrollLoeysingTestregel(
      loeysingId: Int,
      testregelId: Int,
      testgrunnlagId: Int?,
      maalingId: Int?,
  ): Result<Int> {
    return runCatching {
      jdbcTemplate.queryForObject(
          """select count(*) from testlab2_testing.testresultat tr
                where tr.loeysing_id=:loeysingId
                and tr.testregel_id=:testregelId
                and tr.element_resultat = 'brot'
                and (:testgrunnlagId::int is null or testgrunnlag_id=:testgrunnlagId)
                and(:maalingId::int is null or maaling_id=:maalingId)"""
              .trimIndent(),
          mapOf(
              "maalingId" to maalingId,
              "testgrunnlagId" to testgrunnlagId,
              "loeysingId" to loeysingId,
              "testregelId" to testregelId),
          Int::class.java) as Int
    }
  }

  fun getTalBrotForKontrollLoeysingKrav(
      loeysingId: Int,
      testregelIds: List<Int>,
      testgrunnlagId: Int?,
      maalingId: Int?
  ): Result<Int> {
    return runCatching {
      jdbcTemplate.queryForObject(
          """select count(*) from testlab2_testing.testresultat tr
                where tr.loeysing_id=:loeysingId
                and tr.testregel_id in (:testregelIds)
                and tr.element_resultat = 'brot'
                and (:testgrunnlagId::int is null or testgrunnlag_id=:testgrunnlagId)
                and(:maalingId::int is null or maaling_id=:maalingId)"""
              .trimIndent(),
          mapOf(
              "maalingId" to maalingId,
              "testgrunnlagId" to testgrunnlagId,
              "loeysingId" to loeysingId,
              "testregelIds" to testregelIds),
          Int::class.java) as Int
    }
  }

  fun mapSortParamToDBField(sortParam: SortParamTestregel): String {
    return when (sortParam) {
      SortParamTestregel.side -> "url"
      SortParamTestregel.testregel -> "testregel_id"
      SortParamTestregel.elementUtfall -> "element_utfall"
      SortParamTestregel.elementPointer -> "element_omtale_pointer"
    }
  }

  /**
   * Henter testresultat fra testresultat-tabellen og mapper til TestresultatDB, selv om kolonnene
   * ikke nødvendigvis matcher direkte.
   *
   * @param sql SQL-spørring mot testresultat-tabellen
   * @param params Parametre til spørringen
   * @return Liste av TestresultatDB
   */
  fun getTestresultatByTestgrunnlagId(testgrunnlagId: Int): List<TestresultatExport> =
      jdbcTemplate.query(
          """SELECT * FROM testresultat t
               join testlab2_testing.testgrunnlag tg on tg.id=t.testgrunnlag_id
               WHERE testgrunnlag_id = :testgrunnlagId"""
              .trimMargin(),
          MapSqlParameterSource().addValue("testgrunnlagId", testgrunnlagId)) { rs, _ ->
            mapResultSetToTestresultatDBBase(rs)
          }

  fun getTestresultatByMaalingId(maalingId: Int, loeysingId: Int): List<TestresultatExport> =
      jdbcTemplate.query(
          """SELECT * FROM testresultat t
            join testlab2_testing.maalingv1 m on m.id=t.maaling_id 
            WHERE maaling_id = :maalingId and loeysing_id = :loeysingId""",
          MapSqlParameterSource()
              .addValue("maalingId", maalingId)
              .addValue("loeysingId", loeysingId)) { rs, _ ->
            mapResultSetToTestresultatDBBase(rs)
          }

  private fun mapResultSetToTestresultatDBBase(rs: ResultSet): TestresultatExport =
      TestresultatExport(
          testrunUuid = rs.getString("uuid"),
          testregelId = rs.getInt("testregel_id"),
          loeysingId = rs.getInt("loeysing_id"),
          sideutvalId = rs.getInt("crawl_side_id"),
          testUtfoert = rs.getTimestamp("test_vart_utfoert")?.toInstant()
                  ?: java.time.Instant.now(),
          elementUtfall = rs.getString("element_utfall") ?: "",
          elementResultat = rs.getString("element_resultat")?.let { TestresultatUtfall.valueOf(it) }
                  ?: TestresultatUtfall.ikkjeForekomst,
          elementOmtalePointer = rs.getString("element_omtale_pointer") ?: "",
          elementOmtaleHtml = rs.getString("element_omtale_html") ?: "",
          elementOmtaleDescription = rs.getString("element_omtale") ?: "",
          brukarId = rs.getInt("brukar_id"))
}
