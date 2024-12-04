package no.uutilsynet.testlab2testing.loeysing

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@Component
class UtvalDAO(@Autowired val jdbcTemplate: NamedParameterJdbcTemplate) {
  val logger: Logger = LoggerFactory.getLogger(UtvalDAO::class.java)

  @Transactional
  fun createUtval(namn: String, loeysingar: List<Int>): Result<UtvalId> = runCatching {
    val utvalId = insertUtval(namn)
    insertUtvalLoeysingar(loeysingar, utvalId)
    utvalId
  }

  private fun insertUtvalLoeysingar(loeysingar: List<Int>, utvalId: Int) {
    loeysingar.forEach { loeysingId ->
      jdbcTemplate.update(
          """insert into "testlab2_testing"."utval_loeysing" (utval_id, loeysing_id) values (:utvalId, :loeysingId)""",
          mapOf("utvalId" to utvalId, "loeysingId" to loeysingId))
    }
  }

  private fun insertUtval(namn: String): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()

    val mapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("namn", namn)
            .addValue("oppretta", Timestamp.from(Instant.now()))

    jdbcTemplate.update(
        """insert into "testlab2_testing"."utval" (namn, oppretta) values (:namn, :oppretta)""",
        mapSqlParameterSource,
        keyHolder)

    val utvalId = keyHolder.keys?.get("id") as Int

      return utvalId
  }

  data class UtvalFromDatabase(
      val id: Int,
      val namn: String,
      val loeysingar: List<Int>,
      val oppretta: Instant
  )

  fun getUtval(id: Int): Result<UtvalFromDatabase> = runCatching {
    jdbcTemplate.query(
        """
              select utval.id           as utval_id,
                     utval.namn         as utval_namn,
                     utval.oppretta     as oppretta,
                     ul.loeysing_id as loeysing_id
              from "testlab2_testing"."utval"
                       join "testlab2_testing"."utval_loeysing" ul on utval.id = ul.utval_id
              where utval.id = :id
            """
            .trimIndent(),
        mapOf("id" to id),
        ::toUtval)
        ?: throw IllegalArgumentException("Fann ikkje utval med id $id")
  }

  fun getUtvalList(): Result<List<UtvalListItem>> = runCatching {
    jdbcTemplate.query("""select id, namn, oppretta from "testlab2_testing"."utval"""") { rs, _ ->
      UtvalListItem(rs.getInt("id"), rs.getString("namn"), rs.getTimestamp("oppretta").toInstant())
    }
  }

  fun deleteUtval(id: Int): Result<Unit> = runCatching {
    logger.atInfo().log("slettar utval med id $id")
    jdbcTemplate.update(
        """delete from "testlab2_testing"."utval" where id = :id""", mapOf("id" to id))
  }

  private fun toUtval(rs: ResultSet): UtvalFromDatabase {
    require(rs.isBeforeFirst)
    rs.next()
    val id = rs.getInt("utval_id")
    val namn = rs.getString("utval_namn")
    val oppretta = rs.getTimestamp("oppretta").toInstant()
    val loeysingar = mutableListOf<Int>()
    do {
      loeysingar.add(rs.getInt("loeysing_id"))
    } while (rs.next())
    return UtvalFromDatabase(id, namn, loeysingar.toList(), oppretta)
  }
}

typealias UtvalId = Int
