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
                .addKeys("id", "svar_steg")
                .newResultSetExtractor(ResultatManuellKontroll.Ferdig::class.java)
        else
            JdbcTemplateMapperFactory.newInstance()
                .addKeys("id", "svar_steg")
                .ignoreColumns("test_vart_utfoert")
                .newResultSetExtractor(ResultatManuellKontroll.UnderArbeid::class.java)
    val testResultat =
        jdbcTemplate.query(
            """
                select ti.id    as id,
                       ti.sak_id,
                       ti.loeysing_id,
                       ti.testregel_id,
                       ti.nettside_id,
                       ti.element_omtale,
                       ti.element_resultat,
                       ti.element_utfall,
                       ti.test_vart_utfoert,
                       tis.steg as svar_steg,
                       tis.svar as svar_svar
                from testresultat_ik ti
                         left join testresultat_ik_svar tis on ti.id = tis.testresultat_ik_id
                where ti.id = :id
                order by id, svar_steg
            """
                .trimIndent(),
            mapOf("id" to id),
            resultSetExtractor)
    testResultat?.first()!!
  }

  @Transactional
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
    jdbcTemplate.update(
        """
            delete from testresultat_ik_svar
            where testresultat_ik_id = :id
        """
            .trimIndent(),
        mapOf("id" to testResultat.id))
    testResultat.svar.forEach { saveSvar(testResultat.id, it) }
  }

  fun saveSvar(testresultatId: Int, stegOgSvar: ResultatManuellKontroll.Svar): Result<Unit> =
      runCatching {
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
}
