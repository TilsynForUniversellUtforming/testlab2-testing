package no.uutilsynet.testlab2testing.kontroll

import java.time.Instant
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
                               k.utval_id       as utval_id,
                               k.utval_namn     as utval_namn,
                               k.utval_oppretta as utval_oppretta,
                               k.regelsett_id   as regelsett_id
                        from kontroll k
                        where k.id = :id
                        """,
              mapOf("id" to id)) { rm, _ ->
                val kontrollId = rm.getInt("id")

                val utval =
                    rm.getInt("utval_id")
                        .takeIf { it != 0 }
                        ?.let { utvalId ->
                          val utvalNamn = rm.getString("utval_namn")
                          val utvalOppretta = rm.getTimestamp("utval_oppretta").toInstant()
                          val loeysingIdList =
                              jdbcTemplate.queryForList(
                                  "select loeysing_id as id from kontroll_loeysing where kontroll_id = :kontroll_id",
                                  mapOf("kontroll_id" to kontrollId),
                                  Int::class.java)
                          KontrollDB.Utval(
                              utvalId,
                              utvalNamn,
                              utvalOppretta,
                              loeysingIdList.map { KontrollDB.Loeysing(it) })
                        }

                val testreglar =
                    KontrollDB.Testreglar(
                            rm.getInt("regelsett_id").takeUnless { rm.wasNull() },
                            jdbcTemplate.queryForList(
                                "select testregel_id from kontroll_testreglar where kontroll_id = :kontroll_id",
                                mapOf("kontroll_id" to kontrollId),
                                Int::class.java))
                        .takeIf { it.regelsettId != null || it.testregelIdList.isNotEmpty() }

                KontrollDB(
                    rm.getInt("id"),
                    rm.getString("tittel"),
                    rm.getString("saksbehandler"),
                    rm.getString("sakstype"),
                    rm.getString("arkivreferanse"),
                    utval,
                    testreglar)
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
      val utval: Utval?,
      val testreglar: Testreglar?
  ) {
    data class Utval(
        val id: Int,
        val namn: String,
        val oppretta: Instant,
        val loeysingar: List<Loeysing>
    )

    data class Loeysing(val id: Int)

    data class Testreglar(val regelsettId: Int?, val testregelIdList: List<Int>)
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
                    on conflict do nothing
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

  @Transactional
  fun updateKontroll(
      kontroll: Kontroll,
      regelsettId: Int?,
      loeysingIdList: List<Int>
  ): Result<Unit> = runCatching {
    jdbcTemplate.update(
        """
              update kontroll
              set tittel = :tittel,
                  saksbehandler = :saksbehandler,
                  sakstype = :sakstype,
                  arkivreferanse = :arkivreferanse,
                  regelsett_id = :regelsettId
              where kontroll.id = :kontrollId
            """
            .trimIndent(),
        mapOf(
            "tittel" to kontroll.tittel,
            "saksbehandler" to kontroll.saksbehandler,
            "sakstype" to kontroll.sakstype.name,
            "arkivreferanse" to kontroll.arkivreferanse,
            "kontrollId" to kontroll.id,
            "regelsettId" to regelsettId))
    val updateBatchValuesTestreglar =
        loeysingIdList.map { mapOf("kontrollId" to kontroll.id, "testregelId" to it) }

    jdbcTemplate.update(
        "delete from kontroll_testreglar where kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontroll.id, "utvalId" to regelsettId))

    jdbcTemplate.batchUpdate(
        "insert into kontroll_testreglar (kontroll_id, testregel_id) values (:kontrollId, :testregelId)",
        updateBatchValuesTestreglar.toTypedArray())
  }

  @Transactional
  fun updateKontroll(
      kontroll: Kontroll,
      sideutvalLoeysing: SideutvalLoeysing,
  ): Result<Unit> = runCatching {
    jdbcTemplate.update(
        """
              update kontroll
              set tittel = :tittel,
                  saksbehandler = :saksbehandler,
                  sakstype = :sakstype,
                  arkivreferanse = :arkivreferanse,
                  regelsett_id = :regelsettId
              where kontroll.id = :kontrollId
            """
            .trimIndent(),
        mapOf(
            "tittel" to kontroll.tittel,
            "saksbehandler" to kontroll.saksbehandler,
            "sakstype" to kontroll.sakstype.name,
            "arkivreferanse" to kontroll.arkivreferanse,
            "kontrollId" to kontroll.id,
        ))

    val updateBatchValuesSideutval =
        sideutvalLoeysing.sideutval.flatMap { su ->
          su.sideBegrunnelseList.map { side ->
            mapOf(
                "kontroll_id" to kontroll.id,
                "innhaldstype_id" to su.type.id,
                "loeysing_id" to sideutvalLoeysing.loeysingId,
                "innhaldstype_beskrivelse" to (su.type.egendefinertType ?: su.type.innhaldstype),
                "url" to side.url,
                "beskrivelse" to side.begrunnelse)
          }
        }

    jdbcTemplate.update(
        "delete from kontroll_sideutval where kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontroll.id))

    jdbcTemplate.batchUpdate(
        """
          insert into kontroll_sideutval (
            kontroll_id,
            innhaldstype_id,
            loeysing_id,
            innhaldstype_beskrivelse,
            url,
            beskrivelse
          ) values (
            :kontroll_id,
            :innhaldstype_id,
            :loeysing_id,
            :innhaldstype_beskrivelse,
            :url,
            :beskrivelse
          )
          """
            .trimIndent(),
        updateBatchValuesSideutval.toTypedArray())
  }
}
