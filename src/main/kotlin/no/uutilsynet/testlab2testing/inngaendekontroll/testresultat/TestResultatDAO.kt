package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestResultatDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val brukarService: BrukarService
) {
  @Transactional
  fun save(createTestResultat: TestResultatResource.CreateTestResultat): Result<Int> {
    return runCatching {
      val brukarId: Int =
          brukarService.getUserId() ?: throw RuntimeException("No authenticated user")

      val created = Timestamp.from(Instant.now())

      jdbcTemplate.queryForObject(
          """
        insert into testresultat (testgrunnlag_id, loeysing_id, testregel_id, sideutval_id, brukar_id, element_omtale, element_resultat,
                                     element_utfall, test_vart_utfoert, status, kommentar, sist_lagra)
        values (:testgrunnlagId, :loeysingId, :testregelId, :sideutvalId, :brukarId, :elementOmtale, :elementResultat, :elementUtfall,
                :testVartUtfoert,:status, :kommentar, :sist_lagra)
        returning id
      """
              .trimIndent(),
          mapOf(
              "testgrunnlagId" to createTestResultat.testgrunnlagId,
              "loeysingId" to createTestResultat.loeysingId,
              "testregelId" to createTestResultat.testregelId,
              "sideutvalId" to createTestResultat.sideutvalId,
              "brukarId" to brukarId,
              "elementOmtale" to createTestResultat.elementOmtale,
              "elementResultat" to createTestResultat.elementResultat,
              "elementUtfall" to createTestResultat.elementUtfall,
              "kommentar" to createTestResultat.kommentar,
              "testVartUtfoert" to createTestResultat.testVartUtfoert,
              "status" to ResultatManuellKontrollBase.Status.IkkjePaabegynt.name,
              "kommentar" to createTestResultat.kommentar,
              "sist_lagra" to created),
          Int::class.java)!!
    }
  }

  fun getTestResultat(id: Int): Result<ResultatManuellKontroll> =
      getTestResultat(resultatId = id).map { it.first() }

  fun getManyResults(testgrunnlagId: Int): Result<List<ResultatManuellKontroll>> =
      getTestResultat(testgrunnlagId = testgrunnlagId)

  data class SvarDB(val resultatManuellKontrollId: Int, val svar: ResultatManuellKontrollBase.Svar)

  private fun getTestResultat(
      resultatId: Int? = null,
      testgrunnlagId: Int? = null
  ): Result<List<ResultatManuellKontroll>> = runCatching {
    val testResultat =
        jdbcTemplate.query(
            """
                select ti.id    as id,
                       ti.testgrunnlag_id,
                       ti.loeysing_id,
                       ti.testregel_id,
                       ti.sideutval_id,
                       ti.element_omtale,
                       ti.element_resultat,
                       ti.element_utfall,
                       ti.test_vart_utfoert,
                       ti.kommentar,
                       ti.sist_lagra,
                       b.brukarnamn as brukar_brukarnamn,
                       b.namn as brukar_namn,
                       ti.status
                from testresultat ti
                         join brukar b on ti.brukar_id = b.id
                where ${if (resultatId != null) "ti.id = :id" else "true"}
                and ${if (testgrunnlagId != null) "ti.testgrunnlag_id = :testgrunnlag_id" else "true"}
                order by id
            """
                .trimIndent(),
            mapOf("id" to resultatId, "testgrunnlag_id" to testgrunnlagId),
        ) { rs, _ ->
          ResultatManuellKontroll(
              id = rs.getInt("id"),
              testgrunnlagId = rs.getInt("testgrunnlag_id"),
              loeysingId = rs.getInt("loeysing_id"),
              testregelId = rs.getInt("testregel_id"),
              sideutvalId = rs.getInt("sideutval_id"),
              brukar = Brukar(rs.getString("brukar_brukarnamn"), rs.getString("brukar_namn")),
              elementOmtale = rs.getString("element_omtale"),
              elementResultat =
                  runCatching { enumValueOf<TestresultatUtfall>(rs.getString("element_resultat")) }
                      .getOrNull(),
              elementUtfall = rs.getString("element_utfall"),
              svar = emptyList<ResultatManuellKontrollBase.Svar>(),
              testVartUtfoert = rs.getTimestamp("test_vart_utfoert")?.toInstant(),
              status = enumValueOf<ResultatManuellKontrollBase.Status>(rs.getString("status")),
              kommentar = rs.getString("kommentar"),
              sistLagra = rs.getTimestamp("sist_lagra").toInstant())
        }.toList()

    val svarMap =
        jdbcTemplate
            .query(
                """
                select 
                    ti.id,
                      tis.steg,
                       tis.svar
                from testresultat ti
                  join testresultat_svar tis on ti.id = tis.testresultat_id
                where ${if (resultatId != null) "ti.id = :id" else "true"}
                and ${if (testgrunnlagId != null) "ti.testgrunnlag_id = :testgrunnlag_id" else "true"}
                order by id, steg
            """
                    .trimIndent(),
                mapOf("id" to resultatId, "testgrunnlag_id" to testgrunnlagId)) { rs, _ ->
                  SvarDB(
                      rs.getInt("id"),
                      ResultatManuellKontrollBase.Svar(rs.getString("steg"), rs.getString("svar")))
                }
            .groupBy({ it.resultatManuellKontrollId }, { it.svar })

    testResultat.map { it.copy(svar = svarMap[it.id] ?: emptyList()) }
  }

  @Transactional
  fun createRetest(retestResultat: ResultatManuellKontrollBase): Result<Unit> = runCatching {
    val brukarId: Int = brukarService.getUserId() ?: throw RuntimeException("No authenticated user")

    val sistlagra = Timestamp.from(Instant.now())

    val id =
        jdbcTemplate.queryForObject(
            """
        insert into testresultat (testgrunnlag_id, loeysing_id, testregel_id, sideutval_id, brukar_id, element_omtale, element_resultat,
                                     element_utfall, test_vart_utfoert, status, kommentar, sist_lagra)
        values (:testgrunnlagId, :loeysingId, :testregelId, :sideutvalId, :brukarId, :elementOmtale, :elementResultat, :elementUtfall,
                :testVartUtfoert,:status, :kommentar, :sist_lagra)
        returning id
      """
                .trimIndent(),
            mapOf(
                "testgrunnlagId" to retestResultat.testgrunnlagId,
                "loeysingId" to retestResultat.loeysingId,
                "testregelId" to retestResultat.testregelId,
                "sideutvalId" to retestResultat.sideutvalId,
                "brukarId" to brukarId,
                "elementOmtale" to retestResultat.elementOmtale,
                "elementResultat" to retestResultat.elementResultat?.name,
                "elementUtfall" to retestResultat.elementUtfall,
                "testVartUtfoert" to retestResultat.testVartUtfoert?.let { Timestamp.from(it) },
                "status" to retestResultat.status.name,
                "kommentar" to retestResultat.kommentar,
                "sist_lagra" to sistlagra),
            Int::class.java)!!

    saveSvarBatch(id, retestResultat.svar)
  }

  @Transactional
  fun update(testResultat: ResultatManuellKontroll): Result<Unit> = runCatching {
    val now = Timestamp.from(Instant.now())

    val testVartUtfoert =
        if (testResultat.elementOmtale != null &&
            testResultat.elementResultat != null &&
            testResultat.elementUtfall != null)
            now
        else null

    jdbcTemplate.update(
        """
      update testresultat
      set element_omtale    = :elementOmtale,
          element_resultat  = :elementResultat,
          element_utfall    = :elementUtfall,
          test_vart_utfoert = :testVartUtfoert,
          status = :status,
          kommentar = :kommentar,
          sist_lagra = :sist_lagra
      where id = :id
    """
            .trimIndent(),
        mapOf(
            "elementOmtale" to testResultat.elementOmtale,
            "elementResultat" to testResultat.elementResultat?.name,
            "elementUtfall" to testResultat.elementUtfall,
            "testVartUtfoert" to testVartUtfoert,
            "status" to testResultat.status.name,
            "id" to testResultat.id,
            "kommentar" to testResultat.kommentar,
            "sist_lagra" to now))

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

  @Transactional
  fun delete(id: Int): Result<Unit> = runCatching {
    jdbcTemplate.update(
        """
            delete from testresultat
            where id = :id
        """
            .trimIndent(),
        mapOf("id" to id))
  }

  fun saveSvar(testresultatId: Int, stegOgSvar: ResultatManuellKontrollBase.Svar): Result<Unit> =
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

  fun saveSvarBatch(
      testresultatId: Int,
      stegOgSvarList: List<ResultatManuellKontrollBase.Svar>
  ): Result<Unit> = runCatching {
    val batchValues =
        stegOgSvarList.map { (steg, svar) ->
          mapOf("testresultatId" to testresultatId, "steg" to steg, "svar" to svar)
        }

    jdbcTemplate.batchUpdate(
        """
            insert into testresultat_svar (testresultat_id, steg, svar)
            values (:testresultatId, :steg, :svar)
            on conflict (testresultat_id, steg) do update
            set svar = excluded.svar
        """
            .trimIndent(),
        batchValues.toTypedArray())
  }

  @Transactional
  fun saveBilde(testresultatId: Int, bildePath: String, thumbnailPath: String) = runCatching {
    jdbcTemplate.update(
        """
              insert into testresultat_bilde (testresultat_id, bilde, thumbnail, opprettet)
              values (:testresultat_id, :bilde, :thumbnail, :opprettet)
              on conflict (testresultat_id, bilde, thumbnail) do update
              set opprettet = excluded.opprettet;
            """
            .trimMargin(),
        mapOf(
            "testresultat_id" to testresultatId,
            "bilde" to bildePath,
            "thumbnail" to thumbnailPath,
            "opprettet" to Timestamp.from(Instant.now())))
  }

  @Transactional
  fun deleteBilde(bildeId: Int) = runCatching {
    jdbcTemplate.update(
        "delete from testresultat_bilde where id = :bilde_id", mapOf("bilde_id" to bildeId))
  }

  fun getBildeSti(bildeId: Int) = runCatching {
    DataAccessUtils.singleResult(
        jdbcTemplate.query(
            "select id, bilde, thumbnail, opprettet from testresultat_bilde where id = :id",
            mapOf("id" to bildeId),
            DataClassRowMapper.newInstance(BildeSti::class.java)))
  }

  fun getBildePathsForTestresultat(testresultatId: Int) = runCatching {
    jdbcTemplate
        .query(
            "select id, bilde, thumbnail, opprettet from testresultat_bilde where testresultat_id = :testresultat_id",
            mapOf("testresultat_id" to testresultatId),
            DataClassRowMapper.newInstance(BildeSti::class.java))
        .toList()
  }

}
