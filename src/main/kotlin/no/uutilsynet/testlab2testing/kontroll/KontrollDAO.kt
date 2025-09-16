package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2.constants.Kontrolltype
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.sql.ResultSet
import java.time.Instant

@Component
class KontrollDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
    val logger = LoggerFactory.getLogger(KontrollDAO::class.java)


  fun createKontroll(kontroll: KontrollResource.OpprettKontroll): Result<Int> {

    return runCatching {
      val params = MapSqlParameterSource()
      params.addValue("tittel", kontroll.tittel)
      params.addValue("saksbehandler", kontroll.saksbehandler)
      params.addValue("sakstype", kontroll.sakstype.name)
      params.addValue("arkivreferanse", kontroll.arkivreferanse)
      params.addValue("kontrolltype", kontroll.kontrolltype.name)

      val keyHolder: KeyHolder = GeneratedKeyHolder()

      jdbcTemplate.update(
          """
              insert into "testlab2_testing"."kontroll" (tittel, saksbehandler, sakstype, arkivreferanse, kontrolltype)
              values (:tittel, :saksbehandler, :sakstype, :arkivreferanse, :kontrolltype)
              """
              .trimIndent(),
          params,
          keyHolder)
      keyHolder.keys?.get("id") as Int
    }
  }

  fun deleteKontroll(id: Int): Result<Unit> {
    return kotlin.runCatching {
      jdbcTemplate.update(
          """
            delete from "testlab2_testing"."kontroll"
            where id = :id
          """
              .trimIndent(),
          mapOf("id" to id))
    }
  }

  fun getKontroller(): Result<List<KontrollDB>> {
    val ids =
        jdbcTemplate.queryForList(
            """select id from "testlab2_testing"."kontroll"""",
            emptyMap<String, String>(),
            Int::class.java).toList()
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
                           k.oppretta_dato as oppretta_dato,
                           sd.id as styringsdata_id
                    from "testlab2_testing"."kontroll" k
                        left join "testlab2_testing"."styringsdata_kontroll" sd on k.id = sd.kontroll_id
                    where k.id in (:ids)
                    """,
              mapOf("ids" to ids),
          ) { resultSet, _ ->
            val kontrollId = resultSet.getInt("id")
            val utval = getUtval(resultSet, kontrollId)

            val testreglar = getTestreglar(resultSet, kontrollId)

            val sideutvalList = getSideutvalList(kontrollId)

            createKontrollDB(resultSet, utval, testreglar, sideutvalList)
          }.toList()
      if (result.size == ids.size) result
      else
          throw IllegalArgumentException(
              "Noen av kontrollene med id-ene $ids finnes ikke i databasen")
    }
  }

  private fun createKontrollDB(
      resultSet: ResultSet,
      utval: KontrollDB.Utval?,
      testreglar: KontrollDB.Testreglar?,
      sideutvalList: List<Sideutval>
  ) =
      KontrollDB(
          resultSet.getInt("id"),
          resultSet.getString("tittel"),
          resultSet.getString("saksbehandler"),
          resultSet.getString("sakstype"),
          resultSet.getString("arkivreferanse"),
          Kontrolltype.valueOf(resultSet.getString("kontrolltype")),
          utval,
          testreglar,
          sideutvalList,
          resultSet.getTimestamp("oppretta_dato").toInstant(),
          resultSet.getInt("styringsdata_id").takeUnless { resultSet.wasNull() })

  private fun getSideutvalList(kontrollId: Int): List<Sideutval> {
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
                from "testlab2_testing"."kontroll_sideutval"
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
            }.toList()
    return sideutvalList
  }

  private fun getTestreglar(resultSet: ResultSet, kontrollId: Int): KontrollDB.Testreglar? {
    val testreglar =
        KontrollDB.Testreglar(
                resultSet.getInt("regelsett_id").takeUnless { resultSet.wasNull() },
                jdbcTemplate.queryForList(
                    """select testregel_id from "testlab2_testing"."kontroll_testreglar" where kontroll_id = :kontroll_id""",
                    mapOf("kontroll_id" to kontrollId),
                    Int::class.java))
            .takeIf { it.regelsettId != null || it.testregelIdList.isNotEmpty() }
    return testreglar
  }

  private fun getUtval(resultSet: ResultSet, kontrollId: Int): KontrollDB.Utval? {
    val utval =
        resultSet
            .getInt("utval_id")
            .takeIf { it != 0 }
            ?.let { utvalId ->
              val utvalNamn = resultSet.getString("utval_namn")
              val utvalOppretta = resultSet.getTimestamp("utval_oppretta").toInstant()
              val loeysingIdList = getLoeysingIdListForKontroll(kontrollId)
              KontrollDB.Utval(
                  utvalId, utvalNamn, utvalOppretta, loeysingIdList.map { KontrollDB.Loeysing(it) })
            }
    return utval
  }

  private fun getLoeysingIdListForKontroll(kontrollId: Int): List<Int> {
    val loeysingIdList =
        jdbcTemplate.queryForList(
            """select loeysing_id as id from "testlab2_testing"."kontroll_loeysing" where kontroll_id = :kontroll_id""",
            mapOf("kontroll_id" to kontrollId),
            Int::class.java).toList()
    return loeysingIdList
  }

  data class KontrollDB(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: String,
      val arkivreferanse: String,
      val kontrolltype: Kontrolltype,
      val utval: Utval?,
      val testreglar: Testreglar?,
      val sideutval: List<Sideutval> = emptyList(),
      val opprettaDato: Instant = Instant.now(),
      val styringsdataId: Int?
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
              update "testlab2_testing"."kontroll"
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
      updateKontrollTable(kontroll, utvalId)
      updateUtvalLoeysing(kontroll, utvalId)
      updateKontrollLoeysing(kontroll, utvalId)
    }
  }

  private fun updateKontrollLoeysing(kontroll: Kontroll, utvalId: Int) {
    jdbcTemplate.update(
        """
                        delete
                        from "testlab2_testing"."kontroll_loeysing"
                        where kontroll_id = :kontrollId
                        and loeysing_id not in (select loeysing_id from "testlab2_testing"."utval_loeysing" where utval_id = :utvalId)
                    """
            .trimIndent(),
        mapOf("kontrollId" to kontroll.id, "utvalId" to utvalId))
  }

  private fun updateUtvalLoeysing(kontroll: Kontroll, utvalId: Int) {
    jdbcTemplate.update(
        """
                        insert into "testlab2_testing"."kontroll_loeysing" (kontroll_id, loeysing_id)
                        select :kontrollId, loeysing_id
                        from "testlab2_testing"."utval_loeysing"
                        where utval_id = :utvalId
                        on conflict do nothing
                    """
            .trimIndent(),
        mapOf("kontrollId" to kontroll.id, "utvalId" to utvalId))
  }

  private fun updateKontrollTable(kontroll: Kontroll, utvalId: Int) {
    jdbcTemplate.update(
        """
                update "testlab2_testing"."kontroll"
                set tittel = :tittel,
                    saksbehandler = :saksbehandler,
                    sakstype = :sakstype,
                    arkivreferanse = :arkivreferanse,
                    utval_id = utval.id,
                    utval_namn = utval.namn,
                    utval_oppretta = utval.oppretta
                from "testlab2_testing"."utval"
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
  }

  @Transactional
  fun updateKontroll(
      kontroll: Kontroll,
      regelsettId: Int?,
      loeysingIdList: List<Int>
  ): Result<Unit> = runCatching {
    updateKontrollQuery(kontroll, regelsettId)
    deleteFromKontrollTestreglar(kontroll, regelsettId)
    updateKontrollTestreglar(loeysingIdList, kontroll)
  }

  private fun updateKontrollTestreglar(loeysingIdList: List<Int>, kontroll: Kontroll) {
    val updateBatchValuesTestreglar =
        loeysingIdList.map { mapOf("kontrollId" to kontroll.id, "testregelId" to it) }

    jdbcTemplate.batchUpdate(
        """insert into testlab2_testing."kontroll_testreglar" (kontroll_id, testregel_id) values (:kontrollId, :testregelId)""",
        updateBatchValuesTestreglar.toTypedArray())
  }

  private fun deleteFromKontrollTestreglar(kontroll: Kontroll, regelsettId: Int?) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."kontroll_testreglar" where kontroll_id = :kontrollId""",
        mapOf("kontrollId" to kontroll.id, "utvalId" to regelsettId))
  }

  private fun updateKontrollQuery(kontroll: Kontroll, regelsettId: Int?) {
    jdbcTemplate.update(
        """
                  update "testlab2_testing"."kontroll"
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
  }

  @Transactional
  fun updateKontroll(
      kontroll: Kontroll,
      sideutvalBase: List<SideutvalBase>,
  ): Result<Unit> = runCatching {
    deleteFromKontrollSideUtval(kontroll)

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
    jdbcTemplate.batchUpdate(
        """
          insert into "testlab2_testing"."kontroll_sideutval" (
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

  private fun deleteFromKontrollSideUtval(kontroll: Kontroll) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."kontroll_sideutval" where kontroll_id = :kontrollId""",
        mapOf("kontrollId" to kontroll.id))
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
            from "testlab2_testing"."kontroll_sideutval"
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
          """select id, type from testlab2_testing.sideutval_type""",
          DataClassRowMapper.newInstance(SideutvalType::class.java))


    fun getKontrollType(kontrollId:Int): Kontrolltype {
        logger.info("Hentar kontrolltype for kontroll med id $kontrollId")
        jdbcTemplate.queryForObject(
            """select kontrolltype from "testlab2_testing"."kontroll" where id = :kontrollId""",
            mapOf("kontrollId" to kontrollId),
            String::class.java)?.let {
            return Kontrolltype.valueOf(it)
        } ?: throw IllegalArgumentException("Fant ikkje kontroll med id $kontrollId")
    }

}
