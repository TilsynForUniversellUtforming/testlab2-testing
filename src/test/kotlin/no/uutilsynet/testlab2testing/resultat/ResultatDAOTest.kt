package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles

@JdbcTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultatDAOTest() {

  @Autowired private var jdbcTemplate: NamedParameterJdbcTemplate? = null

  @MockBean lateinit var sideutvalDAO: SideutvalDAO

  @MockBean lateinit var brukarService: BrukarService

  @MockBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockBean lateinit var cacheManager: CacheManager

  private var resultatDAO: ResultatDAO? = null

  private var testregelId: Int = 0
  private var utvalId: Int = 0

  @BeforeAll
  fun beforeAll() {
    resultatDAO = ResultatDAO(jdbcTemplate!!)
  }

  @BeforeEach
  fun setUp() {
    testregelId = createTestregel()
  }

  @Test
  fun getTestresultatMaaling() {

    createTestMaalingar(listOf("Forenkla kontroll 20204"))

    val expected1 =
        ResultatLoeysingDTO(
            1,
            testgrunnlagId = 1,
            "Forenkla kontroll 20204",
            Kontrolltype.ForenklaKontroll,
            TestgrunnlagType.OPPRINNELEG_TEST,
            LocalDate.now(),
            listOf(("testar")),
            1,
            0.5,
            6,
            3,
            1)
    val resultat: List<ResultatLoeysingDTO> = resultatDAO!!.getTestresultatMaaling()

    assertThat(resultat).isEqualTo(listOf(expected1))
  }

  @Test
  fun testGetTestresultatMaalingWithParams() {
    val maalingIds =
        createTestMaalingar(listOf("Forenkla kontroll 20204", "Forenkla kontroll 20205"))

    val expected1 =
        ResultatLoeysingDTO(
            1,
            testgrunnlagId = maalingIds[0],
            "Forenkla kontroll 20204",
            Kontrolltype.ForenklaKontroll,
            TestgrunnlagType.OPPRINNELEG_TEST,
            LocalDate.now(),
            listOf(("testar")),
            1,
            0.5,
            6,
            3,
            1)

    val resultat = resultatDAO!!.getTestresultatMaaling(maalingIds[0])

    assertThat(resultat.map { it.testgrunnlagId }).isEqualTo(listOf(expected1.testgrunnlagId))
    assertThat(resultat.map { it.testgrunnlagId }).isNotEqualTo(listOf(maalingIds[1]))
  }

  @Test
  fun getTestresultatTestgrunnlag() {
    val testgrunnlagList =
        listOf(
            OpprettTestgrunnlag("Tilsyn 20204", TestgrunnlagType.OPPRINNELEG_TEST),
            OpprettTestgrunnlag("Tilsyn 20204 Retest", TestgrunnlagType.RETEST))
    val testgrunnlagIds = createTestgrunnlagList(testgrunnlagList)

    val expected =
        ResultatLoeysingDTO(
            1,
            testgrunnlagId = testgrunnlagIds[0],
            "Inngåande kontroll",
            Kontrolltype.InngaaendeKontroll,
            TestgrunnlagType.OPPRINNELEG_TEST,
            LocalDate.now(),
            listOf(("testar")),
            1,
            0.5,
            6,
            3,
            1)

    val resultat = resultatDAO!!.getTestresultatTestgrunnlag()

    assertThat(resultat.size).isEqualTo(2)

    assertThat(resultat[0]).isEqualTo(expected)
  }

  @Test
  fun testGetTestresultatTestgrunnlag() {
    val testgrunnlagList =
        listOf(
            OpprettTestgrunnlag("Tilsyn 20204", TestgrunnlagType.OPPRINNELEG_TEST),
            OpprettTestgrunnlag("Tilsyn 20204 Retest", TestgrunnlagType.RETEST))
    val testgrunnlagIds = createTestgrunnlagList(testgrunnlagList)

    val expected =
        ResultatLoeysingDTO(
            1,
            testgrunnlagId = testgrunnlagIds[0],
            "Inngåande kontroll",
            Kontrolltype.InngaaendeKontroll,
            TestgrunnlagType.OPPRINNELEG_TEST,
            LocalDate.now(),
            listOf(("testar")),
            1,
            0.5,
            6,
            3,
            1)

    val resultat = resultatDAO!!.getTestresultatTestgrunnlag(testgrunnlagId = testgrunnlagIds[0])

    assertThat(resultat.size).isEqualTo(1)

    assertThat(resultat[0]).isEqualTo(expected)
  }

  @Test fun getResultat() {}

  @Test fun getResultatKontroll() {}

  @Test fun handleDate() {}

  @Test fun getResultatKontrollLoeysing() {}

  @Test fun getResultatPrTema() {}

  @Test fun getResultatPrKrav() {}

  private fun createAggregertTestresultat(maalingId: Int?, testregelId: Int, testgrunnlagId: Int?) {
    val aggregeringDAO = AggregeringDAO(jdbcTemplate!!)
    val aggregering_testregel =
        AggregeringPerTestregelDTO(
            maalingId,
            1,
            testregelId,
            1,
            listOf(1, 2),
            6,
            3,
            1,
            1,
            1,
            1,
            0,
            0.5,
            0.5,
            testgrunnlagId)
    aggregeringDAO.createAggregertResultatTestregel(aggregering_testregel)
  }

  fun createTestregel(): Int {

    val testregelDAO = TestregelDAO(jdbcTemplate!!)

    val innholdstypeTesting = testregelDAO.createInnholdstypeTesting("Tekst")

    val testregelInit =
        TestregelInit(
            testregelId = "QW-ACT-R1",
            namn = TestConstants.name,
            kravId = TestConstants.testregelTestKravId,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            testregelSchema = TestConstants.testregelSchemaAutomatisk,
            innhaldstypeTesting = innholdstypeTesting,
            tema = 1,
            testobjekt = 1,
            kravTilSamsvar = "")
    return testregelDAO.createTestregel(testregelInit)
  }

  fun createTestMaaling(testregelIds: List<Int>, kontrollId: Int, maalingNamn: String): Int {
    val maalingDAO =
        MaalingDAO(
            jdbcTemplate!!, loeysingsRegisterClient, sideutvalDAO, brukarService, cacheManager)
    val maalingId =
        maalingDAO.createMaaling(
            maalingNamn, Instant.now(), listOf(1), testregelIds, CrawlParameters())
    maalingDAO.updateKontrollId(maalingId, kontrollId)

    createAggregertTestresultat(maalingId, testregelIds[0], null)

    return maalingId
  }

  fun createTestMaalingar(maalingNamn: List<String>): List<Int> {
    return maalingNamn.map {
      val kontrollId = createKontroll("Forenkla kontroll 20204", Kontrolltype.ForenklaKontroll).id
      createTestMaaling(listOf(testregelId), kontrollId, it)
    }
  }

  fun createKontroll(kontrollNamn: String, kontrolltype: Kontrolltype): KontrollDAO.KontrollDB {

    val (kontrollDAO, kontrollId, kontroll) = opprettKontroll(kontrollNamn, kontrolltype)

    opprettUtvalg(kontrollDAO, kontroll)

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }

  private fun opprettUtvalg(kontrollDAO: KontrollDAO, kontroll: Kontroll) {
    val utvalDAO = UtvalDAO(jdbcTemplate!!)

    val loeysingId = 1
    utvalId = utvalDAO.createUtval("test-skal-slettes", listOf(loeysingId)).getOrThrow()

    kontrollDAO.updateKontroll(kontroll, null, listOf(testregelId))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))
  }

  private fun opprettKontroll(
      kontrollNamn: String,
      kontrolltype: Kontrolltype
  ): Triple<KontrollDAO, Int, Kontroll> {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            kontrollNamn, "Ola Nordmann", Sakstype.Arkivsak, "1234", kontrolltype)

    val kontrollDAO = KontrollDAO(jdbcTemplate!!)

    val kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontrolltype.InngaaendeKontroll,
            opprettKontroll.tittel,
            opprettKontroll.saksbehandler,
            opprettKontroll.sakstype,
            opprettKontroll.arkivreferanse,
        )
    return Triple(kontrollDAO, kontrollId, kontroll)
  }

  private fun createTestgrunnlagList(testgrunnlagList: List<OpprettTestgrunnlag>): List<Int> {
    val kontroll = createKontroll("Inngåande kontroll", Kontrolltype.InngaaendeKontroll)
    return testgrunnlagList.map { createTestgrunnlag(it, kontroll) }
  }

  private fun createTestgrunnlag(
      opprettTestgrunnlag: OpprettTestgrunnlag,
      kontroll: KontrollDAO.KontrollDB
  ): Int {
    val testgrunnlagDAO = TestgrunnlagDAO(jdbcTemplate!!)

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId = kontroll.id,
            namn = opprettTestgrunnlag.testgrunnlagNamn,
            type = opprettTestgrunnlag.testgrunnlagType,
            sideutval = kontroll.sideutval,
            testregelIdList = listOf(testregelId))
    val testgrunnlagId = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag).getOrThrow()
    createAggregertTestresultat(null, testregelId, testgrunnlagId)
    return testgrunnlagId
  }
}

data class OpprettTestgrunnlag(
    val testgrunnlagNamn: String,
    val testgrunnlagType: TestgrunnlagType
)

@Bean
fun namedParameterJdbcTemplate(jdbcTemplate: JdbcTemplate): NamedParameterJdbcTemplate {
  return NamedParameterJdbcTemplate(jdbcTemplate)
}

@Bean
fun restTemlateBuilder(): RestTemplateBuilder {
  return RestTemplateBuilder()
}
