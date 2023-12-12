package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.sql.Timestamp
import java.time.Instant
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
  fun save(createTestResultat: TestResultatResource.CreateTestResultat): Result<Int> {
    return runCatching {
      jdbcTemplate.queryForObject(
          """
        insert into testresultat_ik (sak_id, loeysing_id, testregel_id, nettside_id, element_omtale, element_resultat,
                                     element_utfall, test_vart_utfoert)
        values (:sakId, :loeysingId, :testregelId, :nettsideId, :elementOmtale, :elementResultat, :elementUtfall,
                :testVartUtfoert)
        returning id
      """
              .trimIndent(),
          mapOf(
              "sakId" to createTestResultat.sakId,
              "loeysingId" to createTestResultat.loeysingId,
              "testregelId" to createTestResultat.testregelId,
              "nettsideId" to createTestResultat.nettsideId,
              "elementOmtale" to createTestResultat.elementOmtale,
              "elementResultat" to createTestResultat.elementResultat,
              "elementUtfall" to createTestResultat.elementUtfall,
              "testVartUtfoert" to createTestResultat.testVartUtfoert),
          Int::class.java)!!
    }
  }

  fun saveSvar(testresultatId: Int, stegOgSvar: Pair<String, String>): Result<Unit> = runCatching {
    val (steg, svar) = stegOgSvar

    jdbcTemplate.update(
        """
            insert into testresultat_ik_svar (testresultat_ik_id, steg, svar)
            values (:testresultatId, :steg, :svar)
            on conflict (testresultat_ik_id, steg) do update
            set svar = excluded.svar
        """
            .trimIndent(),
        mapOf("testresultatId" to testresultatId, "steg" to steg, "svar" to svar))
  }

  @Transactional
  fun getTestResultat(id: Int): Result<ResultatManuellKontroll> = runCatching {
    val isFerdig =
        jdbcTemplate.queryForObject(
            "select test_vart_utfoert is not null from testresultat_ik where id = :id",
            mapOf("id" to id),
            Boolean::class.java)!!
    val resultSetExtractor =
        if (isFerdig)
            JdbcTemplateMapperFactory.newInstance()
                .addKeys("id")
                .newResultSetExtractor(ResultatManuellKontroll.Ferdig::class.java)
        else
            JdbcTemplateMapperFactory.newInstance()
                .addKeys("id")
                .ignoreColumns("test_vart_utfoert")
                .newResultSetExtractor(ResultatManuellKontroll.UnderArbeid::class.java)
    val testResultat =
        jdbcTemplate.query(
            """
                select testresultat_ik.id, sak_id, loeysing_id, testregel_id, nettside_id, element_omtale, element_resultat,
                       element_utfall, test_vart_utfoert, steg as svar_steg, svar as svar_svar
                from testresultat_ik
                left join testresultat_ik_svar on testresultat_ik.id = testresultat_ik_svar.testresultat_ik_id
                where testresultat_ik.id = :id
            """
                .trimIndent(),
            mapOf("id" to id),
            resultSetExtractor)
    testResultat?.first()!!
  }

  fun update(testResultat: ResultatManuellKontroll.UnderArbeid): Result<Unit> = runCatching {
    val testVartUtfoert =
        if (testResultat.elementOmtale != null &&
            testResultat.elementResultat != null &&
            testResultat.elementUtfall != null)
            Timestamp.from(Instant.now())
        else null
    jdbcTemplate.update(
        """
      update testresultat_ik
      set element_omtale    = :elementOmtale,
          element_resultat  = :elementResultat,
          element_utfall    = :elementUtfall,
          test_vart_utfoert = :testVartUtfoert
      where id = :id
    """
            .trimIndent(),
        mapOf(
            "elementOmtale" to testResultat.elementOmtale,
            "elementResultat" to testResultat.elementResultat,
            "elementUtfall" to testResultat.elementUtfall,
            "testVartUtfoert" to testVartUtfoert,
            "id" to testResultat.id))
  }
}
