package no.uutilsynet.testlab2testing.styringsdata

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class StyringsdataCleanupDAO(private val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun deleteStyringsdataLoeysingPaaleggByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM styringsdata_loeysing_paalegg slp " +
            "USING styringsdata_loeysing s " +
            "WHERE s.paalegg_id = slp.id " +
            "AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteStyringsdataBotByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM styringsdata_loeysing_bot slp " +
            "USING styringsdata_loeysing s " +
            "WHERE s.bot_id = slp.id " +
            "AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteStyringsdatLoeysingKlageByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM styringsdata_loeysing_klage slk " +
            "USING styringsdata_loeysing s " +
            "WHERE s.paalegg_klage_id = slk.id " +
            "AND kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteStyringdataLoeysingByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM styringsdata_loeysing slbk " + "WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }

  fun deleteStyringdataKontrollByKontrollId(kontrollId: Int): Int {
    return jdbcTemplate.update(
        "DELETE FROM styringsdata_kontroll sk " + "WHERE kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId))
  }
}
