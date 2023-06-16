package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import java.sql.ResultSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UtvalDAO(@Autowired val jdbcTemplate: NamedParameterJdbcTemplate) {
  val logger: Logger = LoggerFactory.getLogger(UtvalDAO::class.java)

  @Transactional
  fun createUtval(namn: String, loeysingar: List<Int>): Result<UtvalId> = runCatching {
    logger.info("lagrer eit nytt utval med namn $namn")
    val utvalId =
        jdbcTemplate.queryForObject(
            "insert into utval (namn) values (:namn) returning id",
            mapOf("namn" to namn),
            Int::class.java)!!
    loeysingar.forEach { loeysingId ->
      jdbcTemplate.update(
          "insert into utval_loeysing (utval_id, loeysing_id) values (:utvalId, :loeysingId)",
          mapOf("utvalId" to utvalId, "loeysingId" to loeysingId))
    }
    utvalId
  }

  fun getUtval(id: Int): Result<Utval> = runCatching {
    jdbcTemplate.query(
        """
              select utval.id as utval_id, utval.namn as utval_namn, loeysing.id as loeysing_id, loeysing.namn as loeysing_namn, loeysing.url as loeysing_url
              from utval
              join utval_loeysing ul on utval.id = ul.utval_id
              join loeysing on loeysing.id = ul.loeysing_id
              where utval.id = :id
            """
            .trimIndent(),
        mapOf("id" to id),
        ::toUtval)
        ?: throw IllegalArgumentException("Fann ikkje utval med id $id")
  }

  fun getUtvalList(): Result<List<UtvalListItem>> = runCatching {
    jdbcTemplate.query("select id, namn from utval") { rs, _ ->
      UtvalListItem(rs.getInt("id"), rs.getString("namn"))
    }
  }

  fun deleteUtval(id: Int): Result<Unit> = runCatching {
    logger.atInfo().log("slettar utval med id $id")
    jdbcTemplate.update("delete from utval where id = :id", mapOf("id" to id))
  }

  private fun toUtval(rs: ResultSet): Utval {
    require(rs.isBeforeFirst)
    rs.next()
    val utval = Utval(rs.getInt("utval_id"), rs.getString("utval_namn"), emptyList())
    val loeysingar = mutableListOf<Loeysing>()
    do {
      loeysingar.add(
          Loeysing(
              rs.getInt("loeysing_id"),
              rs.getString("loeysing_namn"),
              URL(rs.getString("loeysing_url"))))
    } while (rs.next())
    return utval.copy(loeysingar = loeysingar.toList())
  }
}

typealias UtvalId = Int

data class UtvalListItem(val id: Int, val namn: String)
