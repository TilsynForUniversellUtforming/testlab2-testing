package no.uutilsynet.testlab2testing.maaling

import java.net.URI
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

  private fun maalingFromResultSet(rs: ResultSet): Maaling {
    val id = rs.getInt("id")
    val aksjonHref = URI("${locationForId(id)}/status")
    val status = rs.getString("status")
    val navn = rs.getString("navn")
    val url = URL(rs.getString("url"))
    return when (status) {
      "planlegging" -> {
        Maaling.Planlegging(id, navn, url, listOf(Aksjon.StartCrawling(aksjonHref)))
      }
      "crawling" -> Maaling.Crawling(id, navn, url)
      else ->
          throw RuntimeException("Målingen med id = $id er lagret med en ugyldig status: $status")
    }
  }

  fun getMaalinger(): List<Maaling> {
    return jdbcTemplate.query("select * from MaalingV1", { rs, _ -> maalingFromResultSet(rs) })
  }

  fun save(maaling: Maaling): Result<Maaling> {
    val numberOfRows =
        jdbcTemplate.update(
            "update MaalingV1 set navn = ?, status = ? where id = ?",
            maaling.navn,
            Maaling.status(maaling),
            maaling.id)
    return if (numberOfRows == 0) {
      Result.failure(
          IllegalArgumentException("måling med id = ${maaling.id} finnes ikke i databasen"))
    } else {
      Result.success(maaling)
    }
  }
}
