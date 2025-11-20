package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import org.slf4j.LoggerFactory
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestgrunnlagDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  private val logger = LoggerFactory.getLogger(TestgrunnlagDAO::class.java)

  fun getTestgrunnlag(testgrunnlagId: Int): Result<TestgrunnlagKontroll> {

    return getTestgrunnlagKontroll(testgrunnlagId).mapCatching {
      it.copy(
          testreglar = getTestreglarForTestgrunnlag(testgrunnlagId),
          sideutval = getSideutvalForTestgrunnlag(testgrunnlagId))
    }
  }

  fun getSideutvalForTestgrunnlag(testgrunnlagId: Int): List<Sideutval> {
    return jdbcTemplate
        .query(
            """
                  select 
                    ks.id, 
                    ks.sideutval_type_id as type_id,
                    ks.loeysing_id,
                    ks.egendefinert_objekt as egendefinert_type,
                    ks.url,
                    ks.begrunnelse 
                  from "testlab2_testing"."kontroll_sideutval" ks
                    join "testlab2_testing"."testgrunnlag_sideutval_kontroll" tsk on ks.id = tsk.sideutval_id
                  where tsk.testgrunnlag_id = :testgrunnlagId
                """
                .trimIndent(),
            mapOf("testgrunnlagId" to testgrunnlagId),
            DataClassRowMapper.newInstance(Sideutval::class.java))
        .toList()
  }

  private fun getTestreglarForTestgrunnlag(testgrunnlagId: Int): List<Testregel> {
    return jdbcTemplate
        .query(
            """
                  select 
                    t.id,
                    t.krav_id,
                    t.testregel_schema,
                    t.namn,
                    t.modus,
                    t.testregel_id,
                    t.versjon,
                    t.status,
                    t.dato_sist_endra,
                    t.modus,
                    t.spraak,
                    t.tema,
                    t.type,
                    t.testobjekt,
                    t.krav_til_samsvar,
                    t.innhaldstype_testing
                  from "testlab2_testing"."testgrunnlag_testregel_kontroll" ttk 
                  join "testlab2_testing"."testregel" t on t.id = ttk.testregel_id
                    where testgrunnlag_id = :testgrunnlagId
                """
                .trimMargin(),
            mapOf("testgrunnlagId" to testgrunnlagId),
            DataClassRowMapper.newInstance(Testregel::class.java))
        .toList()
  }

  private fun getTestgrunnlagKontroll(testgrunnlagId: Int): Result<TestgrunnlagKontroll> {
    val testgrunnlag =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                """select id, kontroll_id, namn, type, dato_oppretta from "testlab2_testing"."testgrunnlag" where id = :testgrunnlagId""",
                mapOf("testgrunnlagId" to testgrunnlagId),
            ) { rs, _ ->
              TestgrunnlagKontroll(
                  rs.getInt("id"),
                  rs.getInt("kontroll_id"),
                  rs.getString("namn"),
                  emptyList(),
                  emptyList(),
                  TestgrunnlagType.valueOf(rs.getString("type")),
                  rs.getTimestamp("dato_oppretta").toInstant())
            })

    if (testgrunnlag == null) {
      return Result.failure(
          IllegalArgumentException("Testgrunnlag med id $testgrunnlagId finnes ikkje"))
    }
    return Result.success(testgrunnlag)
  }

  fun getOpprinneligTestgrunnlag(kontrollId: Int): Result<Int> = runCatching {
    val result =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                """select id from "testlab2_testing"."testgrunnlag" where kontroll_id = :kontrollId and type = 'OPPRINNELEG_TEST'""",
                mapOf("kontrollId" to kontrollId),
            ) { rs, _ ->
              rs.getInt("id")
            })
    requireNotNull(result) { "Testgrunnlag for kontroll finns ikkje" }

    result
  }

  fun getTestgrunnlagForKontroll(kontrollId: Int): TestgrunnlagList {
    logger.info("Henter testgrunnlag for kontroll $kontrollId")
    val testgrunnlagIds = getTestgrunnlagIds(kontrollId)

    val testgrunnlag =
        testgrunnlagIds.map { id -> getTestgrunnlag(id).getOrThrow() }.groupBy { it.type }
    return TestgrunnlagList(
        testgrunnlag[TestgrunnlagType.OPPRINNELEG_TEST]?.firstOrNull()!!,
        testgrunnlag[TestgrunnlagType.RETEST]?.toList() ?: emptyList())
  }

  private fun getTestgrunnlagIds(kontrollId: Int): List<Int> {
    val testgrunnlagIds =
        jdbcTemplate
            .queryForList(
                """select t.id from "testlab2_testing"."testgrunnlag" t where t.kontroll_id = :kontrollId""",
                mapOf("kontrollId" to kontrollId),
                Int::class.java)
            .toList()
    return testgrunnlagIds
  }

  @Transactional
  fun deleteTestgrunnlag(testgrunnlagId: Int) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."testgrunnlag" where id = :id""",
        mapOf("id" to testgrunnlagId))
  }

  @Transactional
  fun createTestgrunnlag(testgrunnlag: NyttTestgrunnlag): Result<Int> {
    return runCatching {
      val keyHolder = GeneratedKeyHolder()

      val mapSqlParameterSource = MapSqlParameterSource()
      mapSqlParameterSource.addValue("kontrollId", testgrunnlag.kontrollId)
      mapSqlParameterSource.addValue("namn", testgrunnlag.namn)
      mapSqlParameterSource.addValue("type", testgrunnlag.type.name)
      mapSqlParameterSource.addValue("datoOppretta", Timestamp.from(Instant.now()))

      jdbcTemplate.update(
          """
            insert into "testlab2_testing"."testgrunnlag" (kontroll_id, namn, type, dato_oppretta)
            values (:kontrollId, :namn, :type, :datoOppretta)
            """
              .trimMargin(),
          mapSqlParameterSource,
          keyHolder)

      val testgrunnlagId = keyHolder.keys?.get("id") as Int

      saveTestgrunnlagUtval(testgrunnlagId, testgrunnlag.sideutval.map { it.id })
      saveTestgrunnlagTestregel(testgrunnlagId, testgrunnlag.testregelIdList)
      testgrunnlagId
    }
  }

  fun kontrollHasTestresultat(kontrollId: Int): Boolean {
    val resultat =
        jdbcTemplate
            .query(
                """
        select 1 from "testlab2_testing"."testresultat" tr
          join "testlab2_testing"."testgrunnlag" tg on tr.testgrunnlag_id = tg.id
        where tg.kontroll_id = :kontrollId
      """
                    .trimIndent(),
                mapOf("kontrollId" to kontrollId)) { rs, _ ->
                  rs.getInt(1)
                }
            .toList()

    return resultat.isNotEmpty()
  }

  private fun saveTestgrunnlagUtval(testgrunnlagId: Int, sideutvalIdList: List<Int>) {
    val updateBatchValuesRegelsettTestregel =
        sideutvalIdList.map { mapOf("testgrunnlagId" to testgrunnlagId, "sideutvalId" to it) }

    jdbcTemplate.batchUpdate(
        """insert into "testlab2_testing"."testgrunnlag_sideutval_kontroll" (testgrunnlag_id, sideutval_id) values (:testgrunnlagId, :sideutvalId)""",
        updateBatchValuesRegelsettTestregel.toTypedArray())
  }

  private fun saveTestgrunnlagTestregel(testgrunnlagId: Int, testregelIdList: List<Int>) {
    val updateBatchValuesRegelsettTestregel =
        testregelIdList.map { mapOf("testgrunnlagId" to testgrunnlagId, "testregelId" to it) }

    jdbcTemplate.batchUpdate(
        """insert into "testlab2_testing"."testgrunnlag_testregel_kontroll" (testgrunnlag_id, testregel_id) values (:testgrunnlagId, :testregelId)""",
        updateBatchValuesRegelsettTestregel.toTypedArray())
  }

  @Transactional
  fun updateTestgrunnlag(testgrunnlag: TestgrunnlagKontroll): Result<TestgrunnlagKontroll> {
    jdbcTemplate.update(
        """
          update "testlab2_testing"."testgrunnlag" set kontroll_id = :kontrollId, namn = :namn, type = :type
          where id = :id
        """
            .trimMargin(),
        mapOf(
            "id" to testgrunnlag.id,
            "kontrollId" to testgrunnlag.kontrollId,
            "namn" to testgrunnlag.namn,
            "type" to testgrunnlag.type.name))

    updateTestgrunnlagLoeysingNettside(testgrunnlag.id, testgrunnlag.sideutval.map { it.id })
    updateTestgrunnlagTestregel(testgrunnlag.id, testgrunnlag.testreglar.map { it.id })

    return Result.success(testgrunnlag)
  }

  private fun updateTestgrunnlagLoeysingNettside(testgrunnlagId: Int, sideutvalId: List<Int>) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."testgrunnlag_sideutval_kontroll" where testgrunnlag_id = :testgrunnlagId""",
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagUtval(testgrunnlagId, sideutvalId)
  }

  private fun updateTestgrunnlagTestregel(testgrunnlagId: Int, testreglar: List<Int>) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."testgrunnlag_testregel_kontroll" where testgrunnlag_id = :testgrunnlagId""",
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagTestregel(testgrunnlagId, testreglar)
  }

  fun getTestgrunnlagIdForKontroll(kontrollId: Int): Result<Int> {
    return runCatching {
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              "select id from testgrunnlag where kontroll_id = :kontrollId",
              mapOf("kontrollId" to kontrollId),
          ) { rs, _ ->
            rs.getInt("id")
          })
          ?: throw NoSuchElementException("Testgrunnlag for kontroll finns ikkje")
    }
  }
}
