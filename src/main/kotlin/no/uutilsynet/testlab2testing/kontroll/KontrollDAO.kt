package no.uutilsynet.testlab2testing.kontroll

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class KontrollDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
  fun createKontroll(kontroll: KontrollResource.OpprettKontroll): Result<Int> {
    return kotlin.runCatching {
      jdbcTemplate.queryForObject(
          """
            insert into kontroll (tittel, saksbehandler, sakstype, arkivreferanse)
            values (:tittel, :saksbehandler, :sakstype, :arkivreferanse)
            returning id
            """
              .trimIndent(),
          mapOf(
              "tittel" to kontroll.tittel,
              "saksbehandler" to kontroll.saksbehandler,
              "sakstype" to kontroll.sakstype.name,
              "arkivreferanse" to kontroll.arkivreferanse),
          Int::class.java)!!
    }
  }

  fun deleteKontroll(id: Int): Result<Unit> {
    return kotlin.runCatching {
      jdbcTemplate.update(
          """
            delete from kontroll
            where id = :id
          """
              .trimIndent(),
          mapOf("id" to id))
    }
  }

  fun getKontroll(id: Int): Result<OpprettetKontroll?> {
    return runCatching {
      jdbcTemplate.queryForObject(
          """
            select id, tittel, saksbehandler, sakstype, arkivreferanse
            from kontroll
            where id = :id
          """
              .trimIndent(),
          mapOf("id" to id)) { rs, _ ->
            OpprettetKontroll(
                rs.getInt("id"),
                OpprettetKontroll.KontrollType.ManuellKontroll,
                rs.getString("tittel"),
                rs.getString("saksbehandler"),
                OpprettetKontroll.Sakstype.valueOf(rs.getString("sakstype")),
                rs.getString("arkivreferanse"))
          }
    }
  }
}
