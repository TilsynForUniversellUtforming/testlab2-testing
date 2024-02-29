package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.brukar.BrukarDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestResultatDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val brukarDAO: BrukarDAO,
    val testregelDAO: TestregelDAO,
    val kravregisterClient: KravregisterClient,
    val aggregeringDAO: AggregeringDAO
) {
  @Transactional
  fun save(createTestResultat: TestResultatResource.CreateTestResultat): Result<Int> {
    return runCatching {
      val brukarId: Int = brukarDAO.saveBrukar(createTestResultat.brukar)
      jdbcTemplate.queryForObject(
          """
        insert into testresultat (sak_id, loeysing_id, testregel_id, nettside_id, brukar_id, element_omtale, element_resultat,
                                     element_utfall, test_vart_utfoert, status)
        values (:sakId, :loeysingId, :testregelId, :nettsideId, :brukarId, :elementOmtale, :elementResultat, :elementUtfall,
                :testVartUtfoert,:status)
        returning id
      """
              .trimIndent(),
          mapOf(
              "sakId" to createTestResultat.sakId,
              "loeysingId" to createTestResultat.loeysingId,
              "testregelId" to createTestResultat.testregelId,
              "nettsideId" to createTestResultat.nettsideId,
              "brukarId" to brukarId,
              "elementOmtale" to createTestResultat.elementOmtale,
              "elementResultat" to createTestResultat.elementResultat,
              "elementUtfall" to createTestResultat.elementUtfall,
              "testVartUtfoert" to createTestResultat.testVartUtfoert,
              "status" to ResultatManuellKontroll.Status.IkkjePaabegynt.name),
          Int::class.java)!!
    }
  }

  fun getTestResultat(id: Int): Result<ResultatManuellKontroll> =
      getTestResultat(resultatId = id).map { it.first() }

  fun getManyResults(sakId: Int): Result<List<ResultatManuellKontroll>> =
      getTestResultat(sakId = sakId)

  private fun getTestResultat(
      resultatId: Int? = null,
      sakId: Int? = null
  ): Result<List<ResultatManuellKontroll>> = runCatching {
    val resultSetExtractor =
        JdbcTemplateMapperFactory.newInstance()
            .addKeys("id", "svar_steg", "brukar_brukarnamn")
            .newResultSetExtractor(ResultatManuellKontroll::class.java)

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
                       tis.svar as svar_svar,
                       b.brukarnamn as brukar_brukarnamn,
                       b.namn as brukar_namn,
                       ti.status
                from testresultat ti
                         left join testresultat_svar tis on ti.id = tis.testresultat_id
                         join brukar b on ti.brukar_id = b.id
                where ${if (resultatId != null) "ti.id = :id" else "true"}
                and ${if (sakId != null) "ti.sak_id = :sakId" else "true"}
                order by id, svar_steg
            """
                .trimIndent(),
            mapOf("id" to resultatId, "sakId" to sakId),
            resultSetExtractor)
    testResultat ?: emptyList()
  }

  @Transactional
  fun update(testResultat: ResultatManuellKontroll): Result<Unit> = runCatching {
    val testVartUtfoert =
        if (testResultat.elementOmtale != null &&
            testResultat.elementResultat != null &&
            testResultat.elementUtfall != null)
            Timestamp.from(Instant.now())
        else null
    jdbcTemplate.update(
        """
      update testresultat
      set element_omtale    = :elementOmtale,
          element_resultat  = :elementResultat,
          element_utfall    = :elementUtfall,
          test_vart_utfoert = :testVartUtfoert,
          status = :status
      where id = :id
    """
            .trimIndent(),
        mapOf(
            "elementOmtale" to testResultat.elementOmtale,
            "elementResultat" to testResultat.elementResultat,
            "elementUtfall" to testResultat.elementUtfall,
            "testVartUtfoert" to testVartUtfoert,
            "status" to testResultat.status.name,
            "id" to testResultat.id))

    // slett gamle svar og lagre de nye
    jdbcTemplate.update(
        """
            delete from testresultat_svar
            where testresultat_id = :id
        """
            .trimIndent(),
        mapOf("id" to testResultat.id))
    testResultat.svar.forEach { saveSvar(testResultat.id, it) }
  }

    fun delete(id: Int): Result<Unit> = runCatching {
        jdbcTemplate.update(
            """
            delete from testresultat
            where id = :id
        """
                .trimIndent(),
            mapOf("id" to id))
    }

  fun saveSvar(testresultatId: Int, stegOgSvar: ResultatManuellKontroll.Svar): Result<Unit> =
      runCatching {
        val (steg, svar) = stegOgSvar

        jdbcTemplate.update(
            """
            insert into testresultat_svar (testresultat_id, steg, svar)
            values (:testresultatId, :steg, :svar)
            on conflict (testresultat_id, steg) do update
            set svar = excluded.svar
        """
                .trimIndent(),
            mapOf("testresultatId" to testresultatId, "steg" to steg, "svar" to svar))
      }
}
