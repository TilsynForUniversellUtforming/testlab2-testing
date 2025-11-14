package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.sql.ResultSet
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class TestresultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

    val logger = LoggerFactory.getLogger(TestresultatDAO::class.java)

  private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
    TestresultatDB(
        id = rs.getInt("id"),
        testgrunnlagId = rs.getInt("testgrunnlag_id"),
        maalingId = rs.getInt("maaling_id"),
        testregelId = rs.getInt("testregel_id"),
        loeysingId = rs.getInt("loeysing_id"),
        sideutvalId = rs.getInt("crawl_side_id"),
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
                :testregelId, :loeysingId, :crawlSideId, :testUtfoert, :elementUtfall, :elementResultat,
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
            .addValue("crawlSideId", testresultat.sideutvalId)
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
                crawl_side_id = :crawlSideId,
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
            .addValue("crawlSideId", testresultat.sideutvalId)
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
  fun listBy(maalingId: Int, loeysingId: Int?): List<TestresultatDB> {
    val sql =
        "SELECT * FROM testresultat WHERE maaling_id = :maalingId and loeysing_id= :loeysingId and element_resultat= 'brot'"
    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
    return jdbcTemplate.query(sql, params, rowMapper)
  }

  @Observed(name = "List<TestresultatDB> listBy maalingId and loeysingId brot")
  fun listBy(
      maalingId: Int,
      loeysingId: Int?,
      testregelId: Int,
      limit: Int = 20,
      offset: Int = 0
  ): List<TestresultatDB> {
    println("limit: $limit, offset: $offset, maalingId: $maalingId, loeysingId: $loeysingId, testregelId: $testregelId")
    val sql =
        "SELECT * FROM testresultat WHERE maaling_id = :maalingId and loeysing_id= :loeysingId and testregel_Id=:testregelId and element_resultat= 'brot' limit :limit offset :offset"
    val params =
        MapSqlParameterSource()
            .addValue(
                "maalingId",
                maalingId,
            )
            .addValue("loeysingId", loeysingId)
            .addValue("testregelId", testregelId)
            .addValue("limit", limit)
            .addValue("offset", offset)

     val result = runCatching { jdbcTemplate.query(sql, params, rowMapper) }


      return result.getOrThrow()
  }
}
