package no.uutilsynet.testlab2testing.inngaendekontroll

import javax.sql.DataSource
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SakDAO(val jdbcTemplate: NamedParameterJdbcTemplate, val dataSource: DataSource) {
  fun save(sak: Sak): Result<Int> = runCatching {
    jdbcTemplate.queryForObject(
        "insert into sak (virksomhet) values (:virksomhet) returning id",
        mapOf("virksomhet" to sak.virksomhet),
        Int::class.java)!!
  }

  fun getSak(id: Int): Result<Sak> {
    val rowMapper = RowMapper { rs, _ ->
      val array = rs.getArray("loeysingar")?.array as? Array<Int> ?: emptyArray()
      val loeysingar = array.map { Sak.Loeysing(it) }
      Sak(rs.getString("virksomhet"), loeysingar)
    }
    val sak =
        jdbcTemplate.queryForObject(
            """
                select virksomhet, loeysingar
                from sak
                where id = :id
            """
                .trimIndent(),
            mapOf("id" to id),
            rowMapper)
    return if (sak != null) Result.success(sak) else Result.failure(IllegalArgumentException())
  }

  @Transactional
  fun update(id: Int, sak: Sak): Result<Sak> = runCatching {
    val array =
        dataSource.connection.createArrayOf(
            "INTEGER", sak.loeysingar.map { it.loeysingId }.toTypedArray())
    jdbcTemplate.update(
        "update sak set virksomhet = :virksomhet, loeysingar = :loeysingar where id = :id",
        mapOf("virksomhet" to sak.virksomhet, "loeysingar" to array, "id" to id))
    sak
  }
}
