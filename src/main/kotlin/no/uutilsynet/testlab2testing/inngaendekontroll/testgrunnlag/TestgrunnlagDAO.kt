package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestgrunnlagDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun getTestgrunnlag(id: Int): Result<Testgrunnlag> {

    val result =
        jdbcTemplate.query(
            "select id,sak_id,namn,type, dato_oppretta from testgrunnlag where id = :id",
            mapOf("id" to id),
            testgrunnlagRowMapper(id))

    val testgrunnlag: Testgrunnlag =
        result.firstOrNull() ?: return Result.failure(IllegalArgumentException())

    val testreglar =
        jdbcTemplate.queryForList(
            """select testregel_id
              |from testgrunnlag_testregel
              |where testgrunnlag_id = :id
          """
                .trimMargin(),
            mapOf("id" to id),
            Int::class.java)

    return Result.success(testgrunnlag.copy(testreglar = testreglar))
  }

  fun getLoeysing(id: Int): Sak.Loeysing {
    val loeysingar =
        jdbcTemplate.query(
            """select loeysing_id
                  |from testgrunnlag_loeysing_nettside tln join nettside n on tln.nettside_id = n.id
                  |where testgrunnlag_id = :id
              """
                .trimMargin(),
            mapOf("id" to id)) { rs, _ ->
              val nettsider = findNettsiderByTestgrunnlAndLoeysing(id, rs.getInt("loeysing_id"))
              Sak.Loeysing(rs.getInt("loeysing_id"), nettsider)
            }
    return loeysingar.firstOrNull() ?: throw IllegalArgumentException()
  }

  fun getTestgrunnlagForSak(sakId: Int, loeysingId: Int?): List<Testgrunnlag> {
    return jdbcTemplate.queryForList(
        """select t.id, t.sak_id,testgruppering_id,namn,type,dato_oppretta 
                |from testgrunnlag t left join testgrunnlag_loeysing_nettside tln on t.id = tln.testgrunnlag_id 
                |where sak_id = :sakId
                | and ${if (loeysingId != null) "loeysing_id = :loeysingId" else "true"}
                 """
            .trimMargin(),
        mapOf("sakId" to sakId, "loeysingId" to loeysingId),
        Testgrunnlag::class.java)
  }

  @Transactional
  fun createTestgrunnlag(testgrunnlag: NyttTestgrunnlag): Result<Int> {
    val testgrunnlagId =
        jdbcTemplate.queryForObject(
            """insert into testgrunnlag (sak_id, namn, type, dato_oppretta)
                |values (:sakId, :namn, :type, :datoOppretta)
                |returning id
            """
                .trimMargin(),
            mapOf(
                "sakId" to testgrunnlag.parentId,
                "namn" to testgrunnlag.namn,
                "type" to testgrunnlag.type.name,
                "datoOppretta" to Timestamp.from(Instant.now())),
            Int::class.java)

    if (testgrunnlagId != null) {
      saveTestgrunnlagLoeysingNettside(testgrunnlagId, listOf(testgrunnlag.loeysingar))
      saveTestgrunnlagTestregel(testgrunnlagId, testgrunnlag.testregelar)
    }

    return if (testgrunnlagId != null) {
      Result.success(testgrunnlagId)
    } else {
      Result.failure(IllegalArgumentException())
    }
  }

  fun saveTestgrunnlagLoeysingNettside(testgrunnlagId: Int, loeysingar: List<Sak.Loeysing>) {
    loeysingar.forEach() { loeysing ->
      loeysing.nettsider.forEach() { nettside ->
        jdbcTemplate.update(
            """insert into testgrunnlag_loeysing_nettside (testgrunnlag_id, loeysing_id, nettside_id)
                |values (:testgrunnlagId, :loeysingId, :nettsideId)
            """
                .trimMargin(),
            mapOf(
                "testgrunnlagId" to testgrunnlagId,
                "loeysingId" to loeysing.loeysingId,
                "nettsideId" to nettside.id))
      }
    }
  }

  fun saveTestgrunnlagTestregel(testgrunnlagId: Int, testreglar: List<Int>) {
    testreglar.forEach() { testregelId ->
      jdbcTemplate.update(
          """insert into testgrunnlag_testregel (testgrunnlag_id, testregel_id)
                |values (:testgrunnlagId, :testregelId)
            """
              .trimMargin(),
          mapOf("testgrunnlagId" to testgrunnlagId, "testregelId" to testregelId))
    }
  }

  fun updateTestgrunnlag(testgrunnlag: Testgrunnlag): Result<Testgrunnlag> {
    jdbcTemplate.update(
        """update testgrunnlag set sak_id = :sakId, namn = :namn, type = :type
                |where id = :id
            """
            .trimMargin(),
        mapOf(
            "id" to testgrunnlag.id,
            "sakId" to testgrunnlag.parentId,
            "namn" to testgrunnlag.namn,
            "type" to testgrunnlag.type.name))

    updateTestgrunnlagLoeysingNettside(testgrunnlag.id, listOf(testgrunnlag.loeysing))
    updateTestgrunnlagTestregel(testgrunnlag.id, testgrunnlag.testreglar.map { it })

    return Result.success(testgrunnlag)
  }

  fun updateTestgrunnlagLoeysingNettside(testgrunnlagId: Int, loeysingar: List<Sak.Loeysing>) {
    jdbcTemplate.update(
        """delete from testgrunnlag_loeysing_nettside where testgrunnlag_id = :testgrunnlagId"""
            .trimMargin(),
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagLoeysingNettside(testgrunnlagId, loeysingar)
  }

  fun updateTestgrunnlagTestregel(testgrunnlagId: Int, testreglar: List<Int>) {
    jdbcTemplate.update(
        """delete from testgrunnlag_testregel where testgrunnlag_id = :testgrunnlagId"""
            .trimMargin(),
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagTestregel(testgrunnlagId, testreglar)
  }

  fun deleteTestgrunnlag(id: Int) {
    jdbcTemplate.update("delete from testgrunnlag where id = :id", mapOf("id" to id))
  }

  private fun testgrunnlagRowMapper(testgrunnlagId: Int) = RowMapper { rs, _ ->
    Testgrunnlag(
        testgrunnlagId,
        rs.getInt("sak_id"),
        rs.getString("namn"),
        emptyList(),
        getLoeysing(testgrunnlagId),
        Testgrunnlag.TestgrunnlagType.valueOf(rs.getString("type")),
        emptyList(),
        rs.getTimestamp("dato_oppretta").toInstant())
  }

  private fun findNettsiderByTestgrunnlAndLoeysing(
      testgrunnlagId: Int,
      loeysingId: Int
  ): MutableList<Sak.Nettside> =
      jdbcTemplate.query(
          """
                    select id, type, url, beskrivelse, begrunnelse
                    from nettside
                    where id in (
                        select nettside_id
                        from testlab2_testing.testgrunnlag_loeysing_nettside
                        where testgrunnlag_id = :testgrunnlag_id
                            and loeysing_id = :loeysing_id
                    )
                """
              .trimIndent(),
          mapOf("testgrunnlag_id" to testgrunnlagId, "loeysing_id" to loeysingId),
          DataClassRowMapper.newInstance(Sak.Nettside::class.java))
}
