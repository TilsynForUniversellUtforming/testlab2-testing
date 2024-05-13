package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.testregel.Testregel
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestgrunnlagKontrollDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun getTestgrunnlag(testgrunnlagId: Int): Result<TestgrunnlagKontroll> {

    val testgrunnlag =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "select id, kontroll_id, namn, type, dato_oppretta from testgrunnlag where id = :testgrunnlagId",
                mapOf("testgrunnlagId" to testgrunnlagId),
            ) { rs, _ ->
              TestgrunnlagKontroll(
                  rs.getInt("id"),
                  rs.getInt("kontroll_id"),
                  rs.getString("namn"),
                  emptyList(),
                  emptyList(),
                  TestgrunnlagKontroll.TestgrunnlagType.valueOf(rs.getString("type")),
                  emptyList(),
                  rs.getTimestamp("dato_oppretta").toInstant())
            })

    if (testgrunnlag == null) {
      return Result.failure(IllegalArgumentException())
    }

    val testreglar =
        jdbcTemplate.query(
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
              from testgrunnlag_testregel_kontroll ttk 
              join testregel t on t.id = ttk.testregel_id
                where testgrunnlag_id = :testgrunnlagId
            """
                .trimMargin(),
            mapOf("testgrunnlagId" to testgrunnlagId),
            DataClassRowMapper.newInstance(Testregel::class.java))

    val sideutval =
        jdbcTemplate.query(
            """
              select 
                ks.id, 
                ks.sideutval_type_id as type_id,
                ks.loeysing_id,
                ks.egendefinert_objekt as egendefinert_type,
                ks.url,
                ks.begrunnelse 
              from kontroll_sideutval ks
                join testgrunnlag_sideutval_kontroll tsk on ks.id = tsk.sideutval_id
              where tsk.testgrunnlag_id = :testgrunnlagId
            """
                .trimIndent(),
            mapOf("testgrunnlagId" to testgrunnlagId),
            DataClassRowMapper.newInstance(Sideutval::class.java))

    return Result.success(testgrunnlag.copy(testreglar = testreglar, sideutval = sideutval))
  }

  fun getOpprinneligTestgrunnlag(kontrollId: Int): Result<Int> = runCatching {
    val result =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "select id from testgrunnlag where kontroll_id = :kontrollId and type = 'OPPRINNELEG_TEST'",
                mapOf("kontrollId" to kontrollId),
            ) { rs, _ ->
              rs.getInt("id")
            })
    if (result == null) {
      throw IllegalArgumentException()
    }

    result
  }

  fun getTestgrunnlagForKontroll(kontrollId: Int, loeysingId: Int?): List<TestgrunnlagKontroll> {
    logger.info("Henter testgrunnlag for sak $kontrollId og loeysing $loeysingId")
    val testgrunnlagIds =
        jdbcTemplate.queryForList(
            """
          select t.id
          from testgrunnlag t left join testgrunnlag_sideutval_kontroll tsk on t.id = tsk.testgrunnlag_id 
          where t.kontroll_id = :kontrollId
          and ${if (loeysingId != null) "loeysing_id = :loeysingId" else "true"}
        """
                .trimMargin(),
            mapOf("kontrollId" to kontrollId, "loeysingId" to loeysingId),
            Int::class.java)
    return testgrunnlagIds.map { id -> getTestgrunnlag(id).getOrThrow() }
  }

  @Transactional
  fun deleteTestgrunnlag(testgrunnlagId: Int) {
    jdbcTemplate.update("delete from testgrunnlag where id = :id", mapOf("id" to testgrunnlagId))
  }

  @Transactional
  fun createTestgrunnlag(testgrunnlag: NyttTestgrunnlagKontroll): Result<Int> {
    val testgrunnlagId =
        jdbcTemplate.queryForObject(
            """
            insert into testgrunnlag (kontroll_id, namn, type, dato_oppretta)
            values (:kontrollId, :namn, :type, :datoOppretta)
            returning id
            """
                .trimMargin(),
            mapOf(
                "kontrollId" to testgrunnlag.parentId,
                "namn" to testgrunnlag.namn,
                "type" to testgrunnlag.type.name,
                "datoOppretta" to Timestamp.from(Instant.now())),
            Int::class.java)

    if (testgrunnlagId != null) {
      saveTestgrunnlagUtval(testgrunnlagId, testgrunnlag.sideutval.map { it.id })
      saveTestgrunnlagTestregel(testgrunnlagId, testgrunnlag.testregelIdList)
    }

    return if (testgrunnlagId != null) {
      Result.success(testgrunnlagId)
    } else {
      Result.failure(IllegalArgumentException())
    }
  }

  private fun saveTestgrunnlagUtval(testgrunnlagId: Int, sideutvalIdList: List<Int>) {
    val updateBatchValuesRegelsettTestregel =
        sideutvalIdList.map { mapOf("testgrunnlagId" to testgrunnlagId, "sideutvalId" to it) }

    jdbcTemplate.batchUpdate(
        "insert into testgrunnlag_sideutval_kontroll (testgrunnlag_id, sideutval_id) values (:testgrunnlagId, :sideutvalId)",
        updateBatchValuesRegelsettTestregel.toTypedArray())
  }

  private fun saveTestgrunnlagTestregel(testgrunnlagId: Int, testregelIdList: List<Int>) {
    val updateBatchValuesRegelsettTestregel =
        testregelIdList.map { mapOf("testgrunnlagId" to testgrunnlagId, "testregelId" to it) }

    jdbcTemplate.batchUpdate(
        "insert into testgrunnlag_testregel_kontroll (testgrunnlag_id, testregel_id) values (:testgrunnlagId, :testregelId)",
        updateBatchValuesRegelsettTestregel.toTypedArray())
  }

  @Transactional
  fun updateTestgrunnlag(testgrunnlag: TestgrunnlagKontroll): Result<TestgrunnlagKontroll> {
    jdbcTemplate.update(
        """
          update testgrunnlag set kontroll_id = :kontrollId, namn = :namn, type = :type
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
        "delete from testgrunnlag_sideutval_kontroll where testgrunnlag_id = :testgrunnlagId",
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagUtval(testgrunnlagId, sideutvalId)
  }

  private fun updateTestgrunnlagTestregel(testgrunnlagId: Int, testreglar: List<Int>) {
    jdbcTemplate.update(
        "delete from testgrunnlag_testregel_kontroll where testgrunnlag_id = :testgrunnlagId",
        mapOf("testgrunnlagId" to testgrunnlagId))

    saveTestgrunnlagTestregel(testgrunnlagId, testreglar)
  }
}
