package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.aggregering.AggregeringPerTestregelDTO
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.CrawlParameters
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate


@JdbcTest
@ActiveProfiles("test")
class ResultatDAOTest() {

  @Autowired
  private var jdbcTemplate: NamedParameterJdbcTemplate? = null

  @MockBean lateinit var sideutvalDAO: SideutvalDAO

  @MockBean lateinit var brukarService: BrukarService

  @MockBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient;

  @MockBean lateinit var cacheManager: CacheManager



  private  var resultatDAO: ResultatDAO? = null

  private var testregelId: Int = 0

  private var maalingId: Int = 0



  //@MockBean val restTemplateBuilder: RestTemplateBuilder = RestTemplateBuilder()
  @BeforeEach
  fun setUp() {
    resultatDAO = ResultatDAO(jdbcTemplate!!)
      createTestresultat()
  }


  @Test fun getTestresultatMaaling() {

    val expected1: ResultatLoeysing = ResultatLoeysing(1, testgrunnlagId = 1,"Forenkla kontroll 2024",Kontrolltype.ForenklaKontroll,TestgrunnlagType.OPPRINNELEG_TEST,
      LocalDate.now(),
      listOf(("Ola Nordmann"),("Kari Nordmann")),1,0.5,6,3,1,1,"1.1.1 Ikke-tekstlig innhold")
    //val resultat: List<ResultatLoeysing> = resultatDAO.getTestresultatMaaling()


  }

  @Test fun testGetTestresultatMaaling() {}

  @Test fun setTestType() {}

  @Test fun getTestresultatTestgrunnlag() {}

  @Test fun testGetTestresultatTestgrunnlag() {}

  @Test fun getResultat() {}

  @Test fun getResultatKontroll() {}

  @Test fun handleDate() {}

  @Test fun getResultatKontrollLoeysing() {}

  @Test fun getResultatPrTema() {}

  @Test fun getResultatPrKrav() {}

  fun createTestresultat() {

    testregelId = createTestregel()
    maalingId = createTestMaaling(listOf(testregelId))
    val aggregeringDAO = AggregeringDAO(jdbcTemplate!!)
    val aggregering_testregel = AggregeringPerTestregelDTO(maalingId,1,testregelId,1, listOf(1,2),6,2,1,1,1,1,0,0.5,0.5,null)
    aggregeringDAO.createAggregertResultatTestregel(aggregering_testregel)
  }

  fun createTestregel() : Int {


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

  fun createTestMaaling(testregelIds: List<Int>): Int {
    val maalingDAO = MaalingDAO(jdbcTemplate!!, loeysingsRegisterClient,sideutvalDAO,brukarService,cacheManager)
    return maalingDAO.createMaaling("Forenkla kontroll 20204", Instant.now(), listOf(1), testregelIds,CrawlParameters() )
  }
}

@Bean
fun namedParameterJdbcTemplate(jdbcTemplate: JdbcTemplate): NamedParameterJdbcTemplate {
  return NamedParameterJdbcTemplate(jdbcTemplate)
}

@Bean
fun restTemlateBuilder(): RestTemplateBuilder {
  return RestTemplateBuilder()
}
