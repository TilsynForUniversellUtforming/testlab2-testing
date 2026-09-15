package no.uutilsynet.testlab2testing.testresultat

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class TestresultatCleanupDAO(private val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun deleteTestresultatByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM testresultat t USING testlab2_testing.testgrunnlag tg " +
            "WHERE t.testgrunnlag_id=tg.id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteAggregeringSideByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM aggregering_side AS asb USING testlab2_testing.testgrunnlag tg " +
            "WHERE asb.testgrunnlag_id=tg.id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteAggregeringTestregelByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM aggregering_testregel AS at USING testlab2_testing.testgrunnlag tg " +
            "WHERE at.testgrunnlag_id=tg.id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteAggregeringSuksesskriteriumByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM aggregering_suksesskriterium AS ask USING testlab2_testing.testgrunnlag tg " +
            "WHERE ask.testgrunnlag_id=tg.id AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }
}
