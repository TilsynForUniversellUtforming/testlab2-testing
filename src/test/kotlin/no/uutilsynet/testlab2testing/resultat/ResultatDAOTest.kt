package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
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
    @Autowired val testUtils: TestUtils
) {

  val logger = LoggerFactory.getLogger(ResultatDAOTest::class.java)

  private val testresultatIds = mutableMapOf<Int, Kontrolltype>()
  private val resultatPrKontroll = mutableMapOf<Int, Int>()

  private var testregelId: Int = 0

  private var maalingIds = listOf<Int>()
  private var testgrunnlagIds = listOf<Int>()

  @BeforeAll
  fun setup() {
    testregelId = testUtils.createTestregel()
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

    val negatives = resultat.filter { !testgrunnlagIds.contains(it.testgrunnlagId) }.map { it.id }
    logger.info("Negatives testgrunnlag $negatives " + resultat)

    assertThat(resultat.map { it.testType }.filter { it == TestgrunnlagType.OPPRINNELEG_TEST })
        .isNotEmpty()
    assertThat(resultat.map { it.testType }.filter { it == TestgrunnlagType.RETEST }).isNotEmpty()
    assertThat(resultat.map { it.typeKontroll }.contains(Kontrolltype.InngaaendeKontroll)).isTrue()
    assertThat(resultat.map { it.typeKontroll }.filter { it == Kontrolltype.ForenklaKontroll }.size)
        .isEqualTo(0)
    assertThat(resultat.map { it.testgrunnlagId }).containsAll(testgrunnlagIds)
  }

  @Test
  fun testGetTestresultatTestgrunnlag() {

    val resultat = resultatDAO.getTestresultatTestgrunnlag(testgrunnlagId = testgrunnlagIds[0])

    logger.info("Testgrunnlag " + testgrunnlagIds[0] + " " + resultat)

    assertThat(resultat.size).isEqualTo(2)
  }

  @Test
  fun getResultat() {

    println("Testresultatid $testresultatIds")
    logger.info("Testresultatid $testresultatIds")
    logger.info("ResultatPrKontroll " + resultatPrKontroll)

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

    logger.info("ResultatPrKontroll " + resultatPrKontroll)

    val resultat = resultatDAO.getResultatKontroll(existing.kontrollId)

    logger.info(
        "Tesrege l " +
            testregelId +
            " Testgrunnlag " +
            testgrunnlagIds[0] +
            " kontrollId " +
            existing.kontrollId +
            " " +
            resultat)

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
      kontrollId: Int,
      loeysungIds: List<Int> = listOf(1),
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
      resultatPrKontroll[kontrollId] = resultatPrKontroll[kontrollId]?.plus(1) ?: 1
      if (maalingId != null) {
        testresultatIds[id] = Kontrolltype.ForenklaKontroll
      } else {
        testresultatIds[id] = Kontrolltype.InngaaendeKontroll
      }
    }
  }

  fun createTestMaaling(testregelIds: List<Int>, kontrollId: Int, maalingNamn: String): Int {
    val maalingId =
        maalingDAO.createMaaling(
            maalingNamn, Instant.now(), listOf(1), testregelIds, CrawlParameters())
    maalingDAO.updateKontrollId(maalingId, kontrollId)

    createAggregertTestresultat(maalingId, testregelIds[0], null, kontrollId)

    return maalingId
  }

  fun createTestMaalingar(maalingNamn: List<String>): List<Int> {
    return maalingNamn.map {
      val kontrollId =
          testUtils
              .createKontroll(
                  "Forenkla kontroll 20204", Kontrolltype.ForenklaKontroll, listOf(1), testregelId)
              .id
      createTestMaaling(listOf(testregelId), kontrollId, it)
    }
  }

  private fun createTestgrunnlagList(
      testgrunnlagList: List<OpprettTestgrunnlag>,
      loeysingList: List<Int>
  ): List<Int> {
    val kontroll =
        testUtils.createKontroll(
            "Inngåande kontroll", Kontrolltype.InngaaendeKontroll, loeysingList, testregelId)
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

    logger.info("Testgrunnlag id $testgrunnlagId for testregel $testregelId")

    createAggregertTestresultat(
        null, testregelId, testgrunnlagId, kontroll.id, kontroll.sideutval.map { it.loeysingId })
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
