package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.sql.ResultSet
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component

@Component
class MaalingDAO(val jdbcTemplate: JdbcTemplate) {
  fun createMaaling(navn: String, url: URL): Number {
    val id =
        SimpleJdbcInsert(jdbcTemplate)
            .withTableName("MaalingV1")
            .usingGeneratedKeyColumns("id")
            .executeAndReturnKey(
                mapOf("navn" to navn, "url" to url.toString(), "status" to "planlegging"))
    return id
  }

  fun getMaaling(id: Int): Maaling? {
    return DataAccessUtils.singleResult(
        jdbcTemplate.query(
            "select * from MaalingV1 where id = ?", { rs, _ -> maalingFromResultSet(rs) }, id))
  }

  private fun maalingFromResultSet(rs: ResultSet): Maaling.Planlegging {
    val id = rs.getInt("id")
    return Maaling.Planlegging(
        id,
        rs.getString("navn"),
        URL(rs.getString("url")),
        listOf(Aksjon.StartCrawling(locationForId(id))))
  }

  fun getMaalinger(): List<Maaling> {
    return jdbcTemplate.query("select * from MaalingV1", { rs, _ -> maalingFromResultSet(rs) })
  }
}
