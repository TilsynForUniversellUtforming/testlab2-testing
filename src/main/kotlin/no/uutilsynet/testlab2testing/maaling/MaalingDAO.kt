package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.sql.ResultSet
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component

data class MaalingV1(val id: Int, val url: URL)

@Component
class MaalingDAO(val jdbcTemplate: JdbcTemplate) {
  fun createMaaling(url: URL): MaalingV1 {
    val id =
      SimpleJdbcInsert(jdbcTemplate)
        .withTableName("MaalingV1")
        .usingGeneratedKeyColumns("id")
        .executeAndReturnKey(mapOf("url" to url.toString()))
    return MaalingV1(id.toInt(), url)
  }

  fun getMaaling(id: Int): MaalingV1? {
    return DataAccessUtils.singleResult(
      jdbcTemplate.query(
        "select * from MaalingV1 where id = ?", { rs, _ -> maalingFromResultSet(rs) }, id))
  }

  private fun maalingFromResultSet(rs: ResultSet) =
    MaalingV1(rs.getInt("id"), URL(rs.getString("url")))

  fun getMaalinger(): List<MaalingV1> {
    return jdbcTemplate.query("select * from MaalingV1", { rs, _ -> maalingFromResultSet(rs) })
  }
}
