package no.uutilsynet.testlab2testing.kontroll

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
            select k.id as id,
                   k.tittel as tittel,
                   k.saksbehandler as saksbehandler,
                   k.sakstype as sakstype,
                   k.arkivreferanse as arkivreferanse,
                   kl.loeysing_id as loeysingar_id
            from kontroll k
                     left join kontroll_loeysing kl on k.id = kl.kontroll_id
            where k.id = :id
          """
                  .trimIndent(),
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
                        emptyList())
                val loeysingar =
                    rs.getInt("loeysingar_id").let {
                      if (it != 0) mutableListOf(KontrollDB.Loeysing(it)) else mutableListOf()
                    }
                while (rs.next()) {
                  val loeysingId = rs.getInt("loeysingar_id")
                  if (loeysingId != 0) {
                    loeysingar.add(KontrollDB.Loeysing(loeysingId))
                  }
                }
                kontroll.copy(loeysingar = loeysingar.toList())
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
      val loeysingar: List<Loeysing>
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
                arkivreferanse = :arkivreferanse
            where id = :id
          """
              .trimIndent(),
          mapOf(
              "tittel" to kontroll.tittel,
              "saksbehandler" to kontroll.saksbehandler,
              "sakstype" to kontroll.sakstype.name,
              "arkivreferanse" to kontroll.arkivreferanse,
              "id" to kontroll.id))
      kontroll.loeysingar.forEach { loeysing ->
        jdbcTemplate.update(
            """
                insert into kontroll_loeysing (kontroll_id, loeysing_id)
                values (:kontrollId, :loeysingId)
                on conflict (kontroll_id, loeysing_id) do nothing
            """
                .trimIndent(),
            mapOf("kontrollId" to kontroll.id, "loeysingId" to loeysing.id))
      }
      jdbcTemplate.update(
          """
                delete from kontroll_loeysing
                where kontroll_id = :kontrollId
                and loeysing_id not in (:loeysingIds)
            """
              .trimIndent(),
          mapOf("kontrollId" to kontroll.id, "loeysingIds" to kontroll.loeysingar.map { it.id }))
    }
  }
}
