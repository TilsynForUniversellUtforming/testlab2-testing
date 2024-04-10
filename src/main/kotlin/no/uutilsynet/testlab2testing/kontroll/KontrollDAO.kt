package no.uutilsynet.testlab2testing.kontroll

import java.time.Instant
import org.springframework.jdbc.core.ResultSetExtractor
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
          jdbcTemplate.query(
              """
                        select k.id             as id,
                               k.tittel         as tittel,
                               k.saksbehandler  as saksbehandler,
                               k.sakstype       as sakstype,
                               k.arkivreferanse as arkivreferanse,
                               k.utval_id       as utval_id,
                               k.utval_namn     as utval_namn,
                               k.utval_oppretta as utval_oppretta,
                               kl.loeysing_id   as loeysingar_id
                        from kontroll k
                                 left join kontroll_loeysing kl on k.id = kl.kontroll_id
                        where k.id = :id
                        """,
              mapOf("id" to id),
              ResultSetExtractor { rs ->
                rs.next()
                val kontroll =
                    KontrollDB(
                        rs.getInt("id"),
                        rs.getString("tittel"),
                        rs.getString("saksbehandler"),
                        rs.getString("sakstype"),
                        rs.getString("arkivreferanse"),
                        null)
                val utval =
                    rs.getInt("utval_id")
                        .takeIf { it != 0 }
                        ?.let { utvalId ->
                          val utvalNamn = rs.getString("utval_namn")
                          val utvalOppretta = rs.getTimestamp("utval_oppretta").toInstant()
                          val loeysingar =
                              rs.getInt("loeysingar_id").let {
                                if (it != 0) mutableListOf(KontrollDB.Loeysing(it))
                                else mutableListOf()
                              }
                          while (rs.next()) {
                            val loeysingId = rs.getInt("loeysingar_id")
                            if (loeysingId != 0) {
                              loeysingar.add(KontrollDB.Loeysing(loeysingId))
                            }
                          }
                          KontrollDB.Utval(utvalId, utvalNamn, utvalOppretta, loeysingar)
                        }
                kontroll.copy(utval = utval)
              })

      result ?: throw IllegalArgumentException("Fann ikkje kontroll med id $id")
    }
  }

  data class KontrollDB(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: String,
      val arkivreferanse: String,
      val utval: Utval?
  ) {
    data class Utval(
        val id: Int,
        val namn: String,
        val oppretta: Instant,
        val loeysingar: List<Loeysing>
    )

    data class Loeysing(val id: Int)
  }

  @Transactional
  fun updateKontroll(kontroll: Kontroll, utvalId: Int): Result<Unit> {
    return runCatching {
      jdbcTemplate.update(
          """
            update kontroll
            set tittel = :tittel,
                saksbehandler = :saksbehandler,
                sakstype = :sakstype,
                arkivreferanse = :arkivreferanse,
                utval_id = utval.id,
                utval_namn = utval.namn,
                utval_oppretta = utval.oppretta
            from utval
            where kontroll.id = :kontrollId
            and utval.id = :utvalId
          """
              .trimIndent(),
          mapOf(
              "tittel" to kontroll.tittel,
              "saksbehandler" to kontroll.saksbehandler,
              "sakstype" to kontroll.sakstype.name,
              "arkivreferanse" to kontroll.arkivreferanse,
              "kontrollId" to kontroll.id,
              "utvalId" to utvalId))
      jdbcTemplate.update(
          """
                    insert into kontroll_loeysing (kontroll_id, loeysing_id)
                    select :kontrollId, loeysing_id
                    from utval_loeysing
                    where utval_id = :utvalId
                """
              .trimIndent(),
          mapOf("kontrollId" to kontroll.id, "utvalId" to utvalId))
      jdbcTemplate.update(
          """
                    delete
                    from kontroll_loeysing
                    where kontroll_id = :kontrollId
                    and loeysing_id not in (select loeysing_id from utval_loeysing where utval_id = :utvalId)
                """
              .trimIndent(),
          mapOf("kontrollId" to kontroll.id, "utvalId" to utvalId))
    }
  }
}
