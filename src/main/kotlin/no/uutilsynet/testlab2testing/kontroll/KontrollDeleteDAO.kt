package no.uutilsynet.testlab2testing.kontroll

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class KontrollDeleteDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun deleteKontrollLoeysingByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM kontroll_loeysing WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteKontrollTestregelByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM kontroll_testreglar WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteKontrollSideutvalByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM kontroll_sideutval WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteTestgrunnlagByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM testgrunnlag WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteTestgrunnlagLoeysingByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM testgrunnlag_loeysing_nettside tln " +
            "USING testgrunnlag t " +
            "WHERE t.id=tln.testgrunnlag_id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteTestgrunnlagSideutvalKontrollByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM testgrunnlag_sideutval_kontroll tgsk " +
            "USING testgrunnlag t " +
            "WHERE t.id=tgsk.testgrunnlag_id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteTestgrunnlagTestregelByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM testgrunnlag_testregel_kontroll tgt " +
            "USING testgrunnlag t " +
            "WHERE t.id=tgt.testgrunnlag_id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteKontrollByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM kontroll WHERE id = :kontrollId", mapOf("kontrollId" to kontrollId))
  }
}
