package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class TestgrunnlagDeleteDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

    fun deleteTestgrunnlagLoeysingByTestgrunnlagId(testgrunnlagId: Int): Int {
        return jdbcTemplate.update(
            "DELETE FROM testgrunnlag_loeysing_nettside tln " +
                    "WHERE tln.testgrunnlag_id = :testgrunnlagId",
            mapOf("testgrunnlagId" to testgrunnlagId))
    }

    fun deleteTestgrunnlagSideutvalKontrollByTestgrunnlagId(testgrunnlagId: Int): Int {
        return jdbcTemplate.update(
            "DELETE FROM testgrunnlag_sideutval_kontroll tgsk " +
                    "WHERE tgsk.testgrunnlag_id = :testgrunnlagId",
            mapOf("testgrunnlagId" to testgrunnlagId))
    }

    fun deleteTestgrunnlagTestregelByTestgrunnlagId(testgrunnlagId: Int): Int {
        return jdbcTemplate.update(
            "DELETE FROM testgrunnlag_testregel_kontroll tgt " +
                    "WHERE tgt.testgrunnlag_id = :testgrunnlagId",
            mapOf("testgrunnlagId" to testgrunnlagId))
    }

}