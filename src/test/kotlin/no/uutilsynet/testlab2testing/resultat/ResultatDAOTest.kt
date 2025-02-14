package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.time.Instant
import java.time.LocalDate

@SpringBootTest(properties = ["spring.datasource.url= jdbc:tc:postgresql:16-alpine:///test-db"])
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultatDAOTest(
    @Autowired val resultatDAO: ResultatDAO,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val aggregeringDAO: AggregeringDAO,
    @Autowired val maalingDAO: MaalingDAO,
    @Autowired val testregelDAO: TestregelDAO,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val kontrollDAO: KontrollDAO
) {

  val logger = LoggerFactory.getLogger(ResultatDAOTest::class.java)

  private val testresultatIds = mutableMapOf<Int, Kontrolltype>()

  private var testregelId: Int = 0
  private var utvalId: Int = 0

  private var maalingIds = listOf<Int>()
  private var testgrunnlagIds = listOf<Int>()

  @BeforeAll
  fun setup() {
    testregelId = createTestregel()
    maalingIds = createTestMaalingar(listOf("Forenkla kontroll 20204", "Forenkla kontroll 20205"))

    val testgrunnlagList =
        listOf(
            OpprettTestgrunnlag("Tilsyn 20204", TestgrunnlagType.OPPRINNELEG_TEST),
            OpprettTestgrunnlag("Tilsyn 20204 Retest", TestgrunnlagType.RETEST))
    testgrunnlagIds = createTestgrunnlagList(testgrunnlagList, listOf(1, 2))
  }

  @Test
  fun getTestresultatMaaling() {

    val resultat: List<ResultatLoeysingDTO> = resultatDAO.getTestresultatMaaling()

    assertThat(resultat.size).isEqualTo(2)

    resultat.forEach {
      assertThat(it.namn).isEqualTo("Forenkla kontroll 20204")
      assertThat(it.typeKontroll).isEqualTo(Kontrolltype.ForenklaKontroll)
      assertThat(it.typeKontroll).isNotEqualTo(Kontrolltype.InngaaendeKontroll)
      assertThat(it.score).isEqualTo(0.5)
    }
  }

  @Test
  fun testGetTestresultatMaalingWithParams() {

    val expected1 =
        ResultatLoeysingDTO(
            3,
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
            testregelId)

    val resultat = resultatDAO.getTestresultatMaaling(maalingIds[0])

    assertThat(resultat.map { it.testgrunnlagId }).isEqualTo(listOf(expected1.testgrunnlagId))
    assertThat(resultat.map { it.testgrunnlagId }).isNotEqualTo(listOf(maalingIds[1]))
  }

  @Test
  fun getTestresultatTestgrunnlag() {

    val resultat = resultatDAO.getTestresultatTestgrunnlag()

    assertThat(resultat.map { it.testType }.filter { it == TestgrunnlagType.OPPRINNELEG_TEST })
        .isNotEmpty()
    assertThat(resultat.map { it.testType }.filter { it == TestgrunnlagType.RETEST }).isNotEmpty()
    assertThat(resultat.map { it.typeKontroll }.contains(Kontrolltype.InngaaendeKontroll)).isTrue()
    assertThat(resultat.map { it.typeKontroll }.filter { it == Kontrolltype.ForenklaKontroll }.size)
        .isEqualTo(0)
    assertThat(resultat.map { it.testgrunnlagId }.filter { it == testgrunnlagIds[0] }.size)
        .isEqualTo(2)
  }

  @Test
  fun testGetTestresultatTestgrunnlag() {

    val resultat = resultatDAO.getTestresultatTestgrunnlag(testgrunnlagId = testgrunnlagIds[0])

    assertThat(resultat.size).isEqualTo(2)
  }

  @Test
  fun getResultat() {

    println("Testresultatid $testresultatIds")
    logger.info("Testresultatid $testresultatIds")

    val resultat = resultatDAO.getAllResultat()

    assertThat(resultat.size).isEqualTo(6)
    assertThat(resultat.map { it.typeKontroll }).contains(Kontrolltype.ForenklaKontroll)
    assertThat(resultat.map { it.typeKontroll }).contains(Kontrolltype.InngaaendeKontroll)
    assertThat(resultat.map { it.testType }).contains(TestgrunnlagType.RETEST)
    assertThat(resultat.map { it.testType }).contains(TestgrunnlagType.OPPRINNELEG_TEST)
    assertThat(resultat.map { it.namn }).contains("Forenkla kontroll 20204")
    assertThat(resultat.map { it.namn }).contains("Inngåande kontroll")
    assertThat(resultat.map { it.loeysingId }).contains(1)
    assertThat(resultat.map { it.loeysingId }).contains(2)
  }

  @Test
  fun getResultatKontrollWithTestgrunnlagAndMaaling() {

    val resultat = resultatDAO.getAllResultat()

    assertThat(resultat.size).isNotEqualTo(0)
    resultat.map { it.typeKontroll }.contains(Kontrolltype.ForenklaKontroll)
    resultat.map { it.typeKontroll }.contains(Kontrolltype.InngaaendeKontroll)

    resultat.map { it.testType }.contains(TestgrunnlagType.OPPRINNELEG_TEST)
    resultat.map { it.testType }.contains(TestgrunnlagType.RETEST)
  }

  @Test
  fun getResultatKontrollLoeysing() {

    val existing = testgrunnlagDAO.getTestgrunnlag(testgrunnlagIds[0]).getOrThrow()

    val resultat = resultatDAO.getResultatKontrollLoeysing(existing.kontrollId, 2)

    assertThat(resultat.size).isEqualTo(2)
    assertThat(resultat[0].loeysingId).isEqualTo(2)
  }

  @Test
  fun getResultatKontroll() {
    val existing = testgrunnlagDAO.getTestgrunnlag(testgrunnlagIds[0]).getOrThrow()

    val resultat = resultatDAO.getResultatKontroll(existing.kontrollId)

    assertThat(resultat.size).isEqualTo(4)
  }

  @Test
  fun getResultatPrTema() {

    val expected =
        ResultatTema(
            "Bilder",
            50,
            44,
            12,
            24,
            4,
            4,
        )

    val resultat = resultatDAO.getResultatPrTema(null, null, null, null, null)

    assertThat(resultat.size).isEqualTo(1)

    assertThat(resultat[0]).isEqualTo(expected)
  }

  @Test
  fun getResultatPrKrav() {
    val expected = ResultatKravBase(1, 50, 12, 24, 4, 4)

    val resultat = resultatDAO.getResultatPrKrav(null, null, null, null, null)

    assertThat(resultat.size).isEqualTo(1)

    assertThat(resultat[0]).isEqualTo(expected)
  }

  private fun createAggregertTestresultat(
      maalingId: Int?,
      testregelId: Int,
      testgrunnlagId: Int?,
      loeysungIds: List<Int> = listOf(1)
  ) {

    loeysungIds.forEach {
      val aggregeringTestregel =
          AggregeringPerTestregelDTO(
              maalingId,
              it,
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
      val id = aggregeringDAO.createAggregertResultatTestregel(aggregeringTestregel)
      if (maalingId != null) {
        testresultatIds[id] = Kontrolltype.ForenklaKontroll
      } else {
        testresultatIds[id] = Kontrolltype.InngaaendeKontroll
      }
    }
  }

  fun createTestregel(): Int {

    val temaId = testregelDAO.createTema("Bilder")

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
            tema = temaId,
            testobjekt = 1,
            kravTilSamsvar = "")
    return testregelDAO.createTestregel(testregelInit)
  }

  fun createTestMaaling(testregelIds: List<Int>, kontrollId: Int, maalingNamn: String): Int {
    val maalingId =
        maalingDAO.createMaaling(
            maalingNamn, Instant.now(), listOf(1), testregelIds, CrawlParameters())
    maalingDAO.updateKontrollId(maalingId, kontrollId)

    createAggregertTestresultat(maalingId, testregelIds[0], null)

    return maalingId
  }

  fun createTestMaalingar(maalingNamn: List<String>): List<Int> {
    return maalingNamn.map {
      val kontrollId =
          createKontroll("Forenkla kontroll 20204", Kontrolltype.ForenklaKontroll, listOf(1)).id
      createTestMaaling(listOf(testregelId), kontrollId, it)
    }
  }

  fun createKontroll(
      kontrollNamn: String,
      kontrolltype: Kontrolltype,
      loeysingId: List<Int>
  ): KontrollDAO.KontrollDB {

    val (kontrollDAO, kontrollId, kontroll) = opprettKontroll(kontrollNamn, kontrolltype)

    opprettUtvalg(kontrollDAO, kontroll, loeysingId)

    return kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
  }

  private fun opprettUtvalg(
      kontrollDAO: KontrollDAO,
      kontroll: Kontroll,
      loeysingId: List<Int> = listOf(1)
  ) {

    utvalId = utvalDAO.createUtval("test-skal-slettes", loeysingId).getOrThrow()

    val sideUtval =
        loeysingId.map {
          SideutvalBase(it, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null)
        }

    kontrollDAO.updateKontroll(kontroll, null, listOf(testregelId))

    /* Add sideutval */
    kontrollDAO.updateKontroll(kontroll, sideUtval)
  }

  private fun opprettKontroll(
      kontrollNamn: String,
      kontrolltype: Kontrolltype
  ): Triple<KontrollDAO, Int, Kontroll> {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            kontrollNamn, "Ola Nordmann", Sakstype.Arkivsak, "1234", kontrolltype)

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

  private fun createTestgrunnlagList(
      testgrunnlagList: List<OpprettTestgrunnlag>,
      loeysingList: List<Int>
  ): List<Int> {
    val kontroll =
        createKontroll("Inngåande kontroll", Kontrolltype.InngaaendeKontroll, loeysingList)
    return testgrunnlagList.map { createTestgrunnlag(it, kontroll) }
  }

  private fun createTestgrunnlag(
      opprettTestgrunnlag: OpprettTestgrunnlag,
      kontroll: KontrollDAO.KontrollDB
  ): Int {

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId = kontroll.id,
            namn = opprettTestgrunnlag.testgrunnlagNamn,
            type = opprettTestgrunnlag.testgrunnlagType,
            sideutval = kontroll.sideutval,
            testregelIdList = listOf(testregelId))
    val testgrunnlagId = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag).getOrThrow()

    createAggregertTestresultat(
        null, testregelId, testgrunnlagId, kontroll.sideutval.map { it.loeysingId })
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
