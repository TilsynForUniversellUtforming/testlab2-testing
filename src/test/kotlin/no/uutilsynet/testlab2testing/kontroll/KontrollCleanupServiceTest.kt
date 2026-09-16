package no.uutilsynet.testlab2testing.kontroll

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Reaksjonstype
import no.uutilsynet.testlab2testing.kontroll.delete.KontrollCleanupService
import no.uutilsynet.testlab2testing.kontroll.delete.KontrollDeleteDAO
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataCleanupDAO
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatCleanupDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSideDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSuksesskriteriumDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles

@JdbcTest(
    properties =
        [
            "spring.datasource.url= jdbc:tc:postgresql:16-alpine:///KontrollCleanupServiceTest-db",
        ])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(
    KontrollCleanupService::class,
    KontrollDeleteDAO::class,
    TestresultatCleanupDAO::class,
    StyringsdataCleanupDAO::class,
    StyringsdataDAO::class,
    AggregeringDAO::class)
class KontrollCleanupServiceTest(
    @Autowired private val kontrollCleanupService: KontrollCleanupService,
    @Autowired private val styringsdataDAO: StyringsdataDAO,
    @Autowired private val aggregeringDAO: AggregeringDAO,
    @Autowired private val jdbcTemplate: NamedParameterJdbcTemplate,
) {

  @TestConfiguration
  class Beans {
    @Bean fun restTemplateBuilder(): RestTemplateBuilder = RestTemplateBuilder()
  }

  @Test
  fun cleanupKontrollDataDeletesOnlyRowsForSelectedKontrollId() {
    val testregelId =
        requireNotNull(firstTestregelId()) { "Testregel-tabellen er tom i testdatabasen" }

    val targetKontrollId = createKontroll("cleanup-target")
    val otherKontrollId = createKontroll("cleanup-other")
    val targetSideutvalId = createKontrollSideutval(targetKontrollId)
    val otherSideutvalId = createKontrollSideutval(otherKontrollId)

    createKontrollTestregel(targetKontrollId, testregelId)
    createKontrollTestregel(otherKontrollId, testregelId)

    val targetTestgrunnlagId = createTestgrunnlag(targetKontrollId, "target-testgrunnlag")
    val otherTestgrunnlagId = createTestgrunnlag(otherKontrollId, "other-testgrunnlag")

    createTestgrunnlagSideutval(targetTestgrunnlagId, targetSideutvalId)
    createTestgrunnlagSideutval(otherTestgrunnlagId, otherSideutvalId)
    createTestgrunnlagTestregel(targetTestgrunnlagId, testregelId)
    createTestgrunnlagTestregel(otherTestgrunnlagId, testregelId)

    createStyringsdataForKontroll(targetKontrollId)
    createStyringsdataForKontroll(otherKontrollId)

    createAggregeringForTestgrunnlag(targetTestgrunnlagId, testregelId)
    createAggregeringForTestgrunnlag(otherTestgrunnlagId, testregelId)

    jdbcTemplate.update(
        """
        insert into testlab2_testing.testgrunnlag_loeysing_nettside (testgrunnlag_id, loeysing_id)
        values (:testgrunnlagId, :loeysingId)
        """
            .trimIndent(),
        mapOf("testgrunnlagId" to targetTestgrunnlagId, "loeysingId" to 1))

    jdbcTemplate.update(
        """
        insert into testlab2_testing.testgrunnlag_loeysing_nettside (testgrunnlag_id, loeysing_id)
        values (:testgrunnlagId, :loeysingId)
        """
            .trimIndent(),
        mapOf("testgrunnlagId" to otherTestgrunnlagId, "loeysingId" to 1))

    jdbcTemplate.update(
        """
        insert into testlab2_testing.kontroll_loeysing (kontroll_id, loeysing_id)
        values (:kontrollId, :loeysingId)
        """
            .trimIndent(),
        mapOf("kontrollId" to targetKontrollId, "loeysingId" to 1))

    jdbcTemplate.update(
        """
        insert into testlab2_testing.kontroll_loeysing (kontroll_id, loeysing_id)
        values (:kontrollId, :loeysingId)
        """
            .trimIndent(),
        mapOf("kontrollId" to otherKontrollId, "loeysingId" to 1))

    assertHasRowsForKontroll(targetKontrollId)

    kontrollCleanupService.cleanupKontrollData(targetKontrollId)

    assertNoRowsForKontroll(targetKontrollId)
    assertHasRowsForKontroll(otherKontrollId)
  }

  private fun assertHasRowsForKontroll(kontrollId: Int) {
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_testreglar", kontrollId))
        .isGreaterThan(0)
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_sideutval", kontrollId))
        .isGreaterThan(0)
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_loeysing", kontrollId))
        .isGreaterThan(0)
    assertThat(countRowsByKontrollId("testlab2_testing.styringsdata_kontroll", kontrollId))
        .isGreaterThan(0)
    assertThat(countStyringsdataLoeysingRows(kontrollId)).isGreaterThan(0)
    assertThat(countAggregeringRows("aggregering_testregel", kontrollId)).isGreaterThan(0)
    assertThat(countRowsByKontrollId("testlab2_testing.testgrunnlag", kontrollId)).isGreaterThan(0)
  }

  private fun assertNoRowsForKontroll(kontrollId: Int) {
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_testreglar", kontrollId)).isZero()
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_sideutval", kontrollId)).isZero()
    assertThat(countRowsByKontrollId("testlab2_testing.kontroll_loeysing", kontrollId)).isZero()
    assertThat(countRowsByKontrollId("testlab2_testing.styringsdata_kontroll", kontrollId)).isZero()
    assertThat(countStyringsdataLoeysingRows(kontrollId)).isZero()
    assertThat(countAggregeringRows("aggregering_side", kontrollId)).isZero()
    assertThat(countAggregeringRows("aggregering_testregel", kontrollId)).isZero()
    assertThat(countAggregeringRows("aggregering_suksesskriterium", kontrollId)).isZero()
    assertThat(countTestgrunnlagChildRows("testgrunnlag_sideutval_kontroll", kontrollId)).isZero()
    assertThat(countTestgrunnlagChildRows("testgrunnlag_testregel_kontroll", kontrollId)).isZero()
    assertThat(countTestgrunnlagChildRows("testgrunnlag_loeysing_nettside", kontrollId)).isZero()
    assertThat(countRowsByKontrollId("testlab2_testing.testgrunnlag", kontrollId)).isZero()
  }

  private fun firstTestregelId(): Int? {
    return jdbcTemplate.queryForObject(
        "select min(id) from testlab2_testing.testregel", emptyMap<String, Any>(), Int::class.java)
  }

  private fun createKontroll(tittel: String): Int {
    return jdbcTemplate.queryForObject(
        """
        insert into testlab2_testing.kontroll (tittel, saksbehandler, sakstype, arkivreferanse, kontrolltype)
        values (:tittel, :saksbehandler, :sakstype, :arkivreferanse, :kontrolltype)
        returning id
        """
            .trimIndent(),
        mapOf(
            "tittel" to tittel,
            "saksbehandler" to "test",
            "sakstype" to "Arkivsak",
            "arkivreferanse" to "ref-$tittel",
            "kontrolltype" to "InngaaendeKontroll"),
        Int::class.java)!!
  }

  private fun createKontrollSideutval(kontrollId: Int): Int {
    return jdbcTemplate.queryForObject(
        """
        insert into testlab2_testing.kontroll_sideutval (
          kontroll_id, sideutval_type_id, loeysing_id, egendefinert_objekt, url, begrunnelse
        ) values (
          :kontrollId, :sideutvalTypeId, :loeysingId, :egendefinertObjekt, :url, :begrunnelse
        ) returning id
        """
            .trimIndent(),
        mapOf(
            "kontrollId" to kontrollId,
            "sideutvalTypeId" to 1,
            "loeysingId" to 1,
            "egendefinertObjekt" to null,
            "url" to "https://www.example.com",
            "begrunnelse" to "test"),
        Int::class.java)!!
  }

  private fun createKontrollTestregel(kontrollId: Int, testregelId: Int) {
    jdbcTemplate.update(
        """
        insert into testlab2_testing.kontroll_testreglar (kontroll_id, testregel_id)
        values (:kontrollId, :testregelId)
        """
            .trimIndent(),
        mapOf("kontrollId" to kontrollId, "testregelId" to testregelId))
  }

  private fun createTestgrunnlag(kontrollId: Int, namn: String): Int {
    return jdbcTemplate.queryForObject(
        """
        insert into testlab2_testing.testgrunnlag (kontroll_id, namn, type, dato_oppretta)
        values (:kontrollId, :namn, :type, now())
        returning id
        """
            .trimIndent(),
        mapOf("kontrollId" to kontrollId, "namn" to namn, "type" to "OPPRINNELEG_TEST"),
        Int::class.java)!!
  }

  private fun createTestgrunnlagSideutval(testgrunnlagId: Int, sideutvalId: Int) {
    jdbcTemplate.update(
        """
        insert into testlab2_testing.testgrunnlag_sideutval_kontroll (testgrunnlag_id, sideutval_id)
        values (:testgrunnlagId, :sideutvalId)
        """
            .trimIndent(),
        mapOf("testgrunnlagId" to testgrunnlagId, "sideutvalId" to sideutvalId))
  }

  private fun createTestgrunnlagTestregel(testgrunnlagId: Int, testregelId: Int) {
    jdbcTemplate.update(
        """
        insert into testlab2_testing.testgrunnlag_testregel_kontroll (testgrunnlag_id, testregel_id)
        values (:testgrunnlagId, :testregelId)
        """
            .trimIndent(),
        mapOf("testgrunnlagId" to testgrunnlagId, "testregelId" to testregelId))
  }

  private fun createAggregeringForTestgrunnlag(testgrunnlagId: Int, testregelId: Int) {
    aggregeringDAO.createAggregertResultatTestregel(
        AggregeringPerTestregelDB(
            maalingId = null,
            loeysingId = 1,
            testregelId = testregelId,
            testgrunnlagId = testgrunnlagId,
            suksesskriterium = 1,
            fleireSuksesskriterium = listOf(1, 2),
            talElementBrot = 1,
            talElementSamsvar = 2,
            talElementIkkjeForekomst = 0,
            talElementVarsel = 0,
            talSiderBrot = 1,
            talSiderSamsvar = 2,
            talSiderIkkjeForekomst = 0,
            testregelGjennomsnittlegSideBrotProsent = 33.0,
            testregelGjennomsnittlegSideSamsvarProsent = 67.0))

    aggregeringDAO
        .createAggregeringSide(
            AggregeringPerSideDB(
                maalingId = null,
                loeysingId = 1,
                sideUrl = URI("https://www.example.com").toURL(),
                sideNivaa = 1,
                gjennomsnittligBruddProsentTR = 10.0,
                talElementSamsvar = 2,
                talElementBrot = 1,
                talElementVarsel = 0,
                talElementIkkjeForekomst = 0,
                testgrunnlagId = testgrunnlagId))
        .getOrThrow()

    aggregeringDAO.createAggregertResultatSuksesskriterium(
        AggregeringPerSuksesskriteriumDB(
            maalingId = null,
            loeysingId = 1,
            suksesskriteriumId = 1,
            talSiderSamsvar = 2,
            talSiderBrot = 1,
            talSiderIkkjeForekomst = 0,
            testgrunnlagId = testgrunnlagId))
  }

  private fun createStyringsdataForKontroll(kontrollId: Int) {
    styringsdataDAO
        .createStyringsdataKontroll(
            Styringsdata.Kontroll(
                id = null,
                kontrollId = kontrollId,
                ansvarleg = "ansvarleg",
                oppretta = LocalDate.now(),
                frist = LocalDate.now().plusDays(10),
                sistLagra = Instant.now(),
                varselSendtDato = null,
                status = null,
                foerebelsRapportSendtDato = null,
                svarFoerebelsRapportDato = null,
                endeligRapportDato = null,
                kontrollAvsluttaDato = null,
                rapportPublisertDato = null))
        .getOrThrow()

    styringsdataDAO
        .createStyringsdataLoeysing(
            Styringsdata.Loeysing(
                id = null,
                kontrollId = kontrollId,
                ansvarleg = "ansvarleg",
                oppretta = LocalDate.now(),
                frist = LocalDate.now().plusDays(15),
                sistLagra = Instant.now(),
                loeysingId = 1,
                reaksjon = Reaksjonstype.reaksjon,
                paaleggReaksjon = Reaksjonstype.reaksjon,
                paaleggKlageReaksjon = Reaksjonstype.reaksjon,
                botReaksjon = Reaksjonstype.reaksjon,
                botKlageReaksjon = Reaksjonstype.reaksjon,
                paalegg = null,
                paaleggKlage = null,
                bot = null,
                botKlage = null))
        .getOrThrow()
  }

  private fun countRowsByKontrollId(tableName: String, kontrollId: Int): Int {
    return jdbcTemplate.queryForObject(
        "select count(*) from $tableName where kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId),
        Int::class.java)
        ?: 0
  }

  private fun countStyringsdataLoeysingRows(kontrollId: Int): Int {
    return jdbcTemplate.queryForObject(
        "select count(*) from testlab2_testing.styringsdata_loeysing where kontroll_id = :kontrollId",
        mapOf("kontrollId" to kontrollId),
        Int::class.java)
        ?: 0
  }

  private fun countAggregeringRows(tableName: String, kontrollId: Int): Int {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from testlab2_testing.$tableName a
        join testlab2_testing.testgrunnlag tg on tg.id = a.testgrunnlag_id
        where tg.kontroll_id = :kontrollId
        """
            .trimIndent(),
        mapOf("kontrollId" to kontrollId),
        Int::class.java)
        ?: 0
  }

  private fun countTestgrunnlagChildRows(tableName: String, kontrollId: Int): Int {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from testlab2_testing.$tableName child
        join testlab2_testing.testgrunnlag tg on tg.id = child.testgrunnlag_id
        where tg.kontroll_id = :kontrollId
        """
            .trimIndent(),
        mapOf("kontrollId" to kontrollId),
        Int::class.java)
        ?: 0
  }
}
