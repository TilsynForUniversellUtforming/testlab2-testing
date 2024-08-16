package no.uutilsynet.testlab2testing.kontroll

import java.net.URI
import java.time.Instant
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class KontrollDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
  fun createKontroll(kontroll: KontrollResource.OpprettKontroll): Result<Int> {
    return kotlin.runCatching {
      jdbcTemplate.queryForObject(
          """
            insert into kontroll (tittel, saksbehandler, sakstype, arkivreferanse, kontrolltype)
            values (:tittel, :saksbehandler, :sakstype, :arkivreferanse, :kontrolltype)
            returning id
            """
              .trimIndent(),
          mapOf(
              "tittel" to kontroll.tittel,
              "saksbehandler" to kontroll.saksbehandler,
              "sakstype" to kontroll.sakstype.name,
              "arkivreferanse" to kontroll.arkivreferanse,
              "kontrolltype" to kontroll.kontrolltype.name),
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

  fun getKontroller(): Result<List<KontrollDB>> {
    val ids =
        jdbcTemplate.queryForList(
            "select id from kontroll", emptyMap<String, String>(), Int::class.java)
    return getKontroller(ids)
  }

  fun getKontroller(ids: List<Int>): Result<List<KontrollDB>> {
    if (ids.isEmpty()) {
      return Result.success(emptyList())
    }

    return runCatching {
      val result =
          jdbcTemplate.query(
              """
                    select k.id             as id,
                           k.tittel         as tittel,
                           k.saksbehandler  as saksbehandler,
                           k.sakstype       as sakstype,
                           k.arkivreferanse as arkivreferanse,
                           k.kontrolltype   as kontrolltype,
                           k.utval_id       as utval_id,
                           k.utval_namn     as utval_namn,
                           k.utval_oppretta as utval_oppretta,
                           k.regelsett_id   as regelsett_id,
                           k.oppretta_dato as oppretta_dato
                    from kontroll k
                    where k.id in (:ids)
                    """,
              mapOf("ids" to ids),
          ) { resultSet, _ ->
            val kontrollId = resultSet.getInt("id")

            val utval =
                resultSet
                    .getInt("utval_id")
                    .takeIf { it != 0 }
                    ?.let { utvalId ->
                      val utvalNamn = resultSet.getString("utval_namn")
                      val utvalOppretta = resultSet.getTimestamp("utval_oppretta").toInstant()
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
                        resultSet.getInt("regelsett_id").takeUnless { resultSet.wasNull() },
                        jdbcTemplate.queryForList(
                            "select testregel_id from kontroll_testreglar where kontroll_id = :kontroll_id",
                            mapOf("kontroll_id" to kontrollId),
                            Int::class.java))
                    .takeIf { it.regelsettId != null || it.testregelIdList.isNotEmpty() }

            val sideutvalList =
                jdbcTemplate.query(
                    """
            select
            id,
            sideutval_type_id,
            loeysing_id,
            egendefinert_objekt,
            url,
            begrunnelse
            from kontroll_sideutval
            where kontroll_id = :kontroll_id
          """
                        .trimIndent(),
                    mapOf("kontroll_id" to kontrollId)) { mapper, _ ->
                      Sideutval(
                          id = mapper.getInt("id"),
                          loeysingId = mapper.getInt("loeysing_id"),
                          typeId = mapper.getInt("sideutval_type_id"),
                          begrunnelse = mapper.getString("begrunnelse"),
                          url = URI(mapper.getString("url")),
                          egendefinertType = mapper.getString("egendefinert_objekt"))
                    }

            KontrollDB(
                resultSet.getInt("id"),
                resultSet.getString("tittel"),
                resultSet.getString("saksbehandler"),
                resultSet.getString("sakstype"),
                resultSet.getString("arkivreferanse"),
                Kontroll.Kontrolltype.valueOf(resultSet.getString("kontrolltype")),
                utval,
                testreglar,
                sideutvalList,
                resultSet.getTimestamp("oppretta_dato").toInstant())
          }
      if (result.size == ids.size) result
      else
          throw IllegalArgumentException(
              "Noen av kontrollene med id-ene $ids finnes ikke i databasen")
    }
  }

  data class KontrollDB(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: String,
      val arkivreferanse: String,
      val kontrolltype: Kontroll.Kontrolltype,
      val utval: Utval?,
      val testreglar: Testreglar?,
      val sideutval: List<Sideutval> = emptyList(),
      val opprettaDato: Instant = Instant.now(),
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
  fun updateKontroll(
      kontroll: Kontroll,
  ): Result<Unit> = runCatching {
    jdbcTemplate.update(
        """
              update kontroll
              set tittel = :tittel,
                  saksbehandler = :saksbehandler,
                  sakstype = :sakstype,
                  arkivreferanse = :arkivreferanse
              where kontroll.id = :kontrollId
            """
            .trimIndent(),
        mapOf(
            "tittel" to kontroll.tittel,
            "saksbehandler" to kontroll.saksbehandler,
            "sakstype" to kontroll.sakstype.name,
            "arkivreferanse" to kontroll.arkivreferanse,
            "kontrollId" to kontroll.id))
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
      sideutvalBase: List<SideutvalBase>,
  ): Result<Unit> = runCatching {
    val updateBatchValuesSideutval =
        sideutvalBase.map { side ->
          mapOf(
              "kontroll_id" to kontroll.id,
              "sideutval_type_id" to side.typeId,
              "loeysing_id" to side.loeysingId,
              "egendefinert_objekt" to side.egendefinertType,
              "url" to side.url.toString(),
              "begrunnelse" to side.begrunnelse)
        }

    jdbcTemplate.update(
        "delete from kontroll_sideutval where kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontroll.id))

    jdbcTemplate.batchUpdate(
        """
          insert into kontroll_sideutval (
            kontroll_id,
            sideutval_type_id,
            loeysing_id,
            egendefinert_objekt,
            url,
            begrunnelse
          ) values (
            :kontroll_id,
            :sideutval_type_id,
            :loeysing_id,
            :egendefinert_objekt,
            :url,
            :begrunnelse
          )
          """
            .trimIndent(),
        updateBatchValuesSideutval.toTypedArray())
  }

  fun findSideutvalByKontrollAndLoeysing(
      kontrollId: Int,
      loeysingIdList: List<Int>
  ): List<Sideutval> =
      jdbcTemplate.query(
          """
            select
              id, 
              kontroll_id,
              sideutval_type_id,
              loeysing_id,
              egendefinert_objekt,
              url,
              begrunnelse
            from kontroll_sideutval
              where kontroll_id = :kontrollId and loeysing_id in (:loeysingIdList)
          """
              .trimIndent(),
          mapOf("kontrollId" to kontrollId, "loeysingIdList" to loeysingIdList),
      ) { mapper, _ ->
        Sideutval(
            id = mapper.getInt("id"),
            loeysingId = mapper.getInt("loeysing_id"),
            typeId = mapper.getInt("sideutval_type_id"),
            begrunnelse = mapper.getString("begrunnelse"),
            url = URI(mapper.getString("url")),
            egendefinertType = mapper.getString("egendefinert_objekt"))
      }

  fun getSideutvalType(): List<SideutvalType> =
      jdbcTemplate.query(
          "select id, type from sideutval_type",
          DataClassRowMapper.newInstance(SideutvalType::class.java))
}
