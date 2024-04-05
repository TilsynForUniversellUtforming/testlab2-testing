package no.uutilsynet.testlab2testing.kontroll

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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

  fun getKontroll(id: Int): Result<KontrollDB> {
    return runCatching {
      val result =
          jdbcTemplate.queryForObject(
              """
            select k.id             as id,
                   k.tittel         as tittel,
                   k.saksbehandler  as saksbehandler,
                   k.sakstype       as sakstype,
                   k.arkivreferanse as arkivreferanse,
                   k.utval_id
            from kontroll k
            where k.id = :id
          """
                  .trimIndent(),
              mapOf("id" to id)) { rs, _ ->
                KontrollDB(
                    rs.getInt("id"),
                    rs.getString("tittel"),
                    rs.getString("saksbehandler"),
                    rs.getString("sakstype"),
                    rs.getString("arkivreferanse"),
                    rs.getInt("utval_id").let { if (it != 0) it else null },
                )
              }

      result ?: throw IllegalArgumentException("Fann ikkje kontroll med id $id")
    }
  }

  data class KontrollDB(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: String,
      val arkivreferanse: String,
      val utvalId: Int?
  ) {
    data class Loeysing(val id: Int)
  }

  @Transactional
  fun updateKontroll(kontroll: Kontroll): Result<Unit> {
    return runCatching {
      jdbcTemplate.update(
          """
            update kontroll
            set tittel = :tittel,
                saksbehandler = :saksbehandler,
                sakstype = :sakstype,
                arkivreferanse = :arkivreferanse,
                utval_id = :utvalId
            where id = :id
          """
              .trimIndent(),
          mapOf(
              "tittel" to kontroll.tittel,
              "saksbehandler" to kontroll.saksbehandler,
              "sakstype" to kontroll.sakstype.name,
              "arkivreferanse" to kontroll.arkivreferanse,
              "utvalId" to kontroll.utval?.id,
              "id" to kontroll.id))
    }
  }
}
