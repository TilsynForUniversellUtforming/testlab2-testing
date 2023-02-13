package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.sql.ResultSet
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component

sealed class Maaling {
  data class IkkeStartet(val id: Int, val navn: String, val url: URL) : Maaling()
}

@Component
class MaalingDAO(val jdbcTemplate: JdbcTemplate) {
  fun createMaaling(navn: String, url: URL): Maaling.IkkeStartet {
    val id =
        SimpleJdbcInsert(jdbcTemplate)
            .withTableName("MaalingV1")
            .usingGeneratedKeyColumns("id")
            .executeAndReturnKey(
                mapOf("navn" to navn, "url" to url.toString(), "status" to "ikke_startet"))
    return Maaling.IkkeStartet(id.toInt(), navn, url)
  }

  fun getMaaling(id: Int): Maaling? {
    return DataAccessUtils.singleResult(
        jdbcTemplate.query(
            "select * from MaalingV1 where id = ?", { rs, _ -> maalingFromResultSet(rs) }, id))
  }

  private fun maalingFromResultSet(rs: ResultSet) =
      Maaling.IkkeStartet(rs.getInt("id"), rs.getString("navn"), URL(rs.getString("url")))

  fun getMaalinger(): List<Maaling> {
    return jdbcTemplate.query("select * from MaalingV1", { rs, _ -> maalingFromResultSet(rs) })
  }
}
