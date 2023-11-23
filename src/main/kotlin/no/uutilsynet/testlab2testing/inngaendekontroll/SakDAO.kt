package no.uutilsynet.testlab2testing.inngaendekontroll

import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class SakDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
  fun save(sak: Sak): Result<Int> = runCatching {
    jdbcTemplate.update(
        "insert into sak (virksomhet) values (:virksomhet)", mapOf("virksomhet" to sak.virksomhet))
  }

  fun getSak(id: Int): Result<Sak> {
    val sakRowMapper = DataClassRowMapper.newInstance(Sak::class.java)
    val sak =
        jdbcTemplate.queryForObject(
            "select * from sak where id = :id", mapOf("id" to id), sakRowMapper)
    return if (sak != null) Result.success(sak) else Result.failure(IllegalArgumentException())
  }
}
