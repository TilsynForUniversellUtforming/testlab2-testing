package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.crawlResultat
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.testKoeyringList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.uutilsynetLoeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.assertj.core.api.Assertions
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import java.net.URI
import java.time.Instant

@RestClientTest(AutoTesterClient::class, AutoTesterProperties::class)
class MaalingResourceMockedTest {

  @Autowired private lateinit var server: MockRestServiceServer
  @Autowired private lateinit var autoTesterClient: AutoTesterClient

  @MockitoBean private lateinit var maalingDAO: MaalingDAO

  @MockitoBean private lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockitoBean
  private lateinit var testregelDAO: TestregelDAO

  @MockitoBean private lateinit var utvalDAO: UtvalDAO

  @MockitoBean private lateinit var crawlerClient: CrawlerClient

  @MockitoBean private lateinit var aggregeringService: AggregeringService

  @MockitoBean private lateinit var sideutvalDAO: SideutvalDAO

  @MockitoBean private lateinit var brukarService: BrukarService

  private lateinit var maalingResource: MaalingResource

  private val objectMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @BeforeEach
  fun setup() {
    MockitoAnnotations.openMocks(this)
    maalingResource =
        MaalingResource(
            maalingDAO,
            loeysingsRegisterClient,
            testregelDAO,
            crawlerClient,
            autoTesterClient,
            aggregeringService,
            sideutvalDAO,
            MaalingService(
                maalingDAO, loeysingsRegisterClient, testregelDAO, utvalDAO, aggregeringService, autoTesterClient),
            brukarService)
  }

    @Test
  @DisplayName("Skal returnere feil når man bruker ulovlig testregelSchema")
  fun illegaltestregelSchema() {
    val id = 1
    val status = MaalingResource.StatusDTO("testing", null)
    val maaling =
        Maaling.Kvalitetssikring(id, "Testmaaling", maalingDateStart, listOf(crawlResultat))

    `when`(maalingDAO.getMaaling(id)).thenReturn(maaling)
    `when`(testregelDAO.getTestreglarForMaaling(id))
        .thenReturn(
            Result.success(
                listOf(
                    Testregel(
                        1,
                        "QW",
                        1,
                        "name",
                        1,
                        TestregelStatus.publisert,
                        Instant.now(),
                        TestregelInnholdstype.nett,
                        TestregelModus.automatisk,
                        TestlabLocale.nb,
                        1,
                        1,
                        "QW",
                        "1.2.3",
                        1))))

    val result = maalingResource.putNewStatus(id, status)

    Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    Assertions.assertThat(result.body).isEqualTo("QualWeb regel id må vera på formen QW-ACT-RXX")
  }

  @Test
  @DisplayName("Skal kunne starte testing på nytt")
  fun restartTesting() {
    val id = 1
    val status = MaalingResource.StatusDTO("testing", listOf(uutilsynetLoeysing.id))
    val maaling = Maaling.TestingFerdig(id, "Testmaaling", maalingDateStart, testKoeyringList)

    val maalingTesting =
        Maaling.Testing(id, "Testmaaling", maalingDateStart, listOf(testKoeyringList[0]))

    val testregelList =
        listOf(
            Testregel(
                1,
                "QW-ACT-R12",
                1,
                "name",
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.automatisk,
                TestlabLocale.nb,
                1,
                1,
                "QW-ACT-R12",
                "QW-ACT-R12",
                1))

    val nettsider =
        listOf(
            URI("https://www.uutilsynet.no/").toURL(),
            URI("https://www.uutilsynet.no/underside/1").toURL(),
            URI("https://www.uutilsynet.no/underside/2").toURL())

    val expectedRequestData =
        mapOf(
            "urls" to nettsider,
            "idMaaling" to id,
            "idLoeysing" to uutilsynetLoeysing.id,
            "resultatSomFil" to true,
            "actRegler" to testregelList.map { it.testregelSchema },
        )

    val statusUris = AutoTesterClient.StatusUris(URI(TestConstants.statusURL))
    val jsonResponse = jacksonObjectMapper().writeValueAsString(statusUris)

    `when`(maalingDAO.getMaaling(id)).thenReturn(maaling)
    `when`(maalingDAO.save(maalingTesting)).thenReturn(Result.success(maalingTesting))
    `when`(sideutvalDAO.getCrawlResultatNettsider(id, uutilsynetLoeysing.id)).thenReturn(nettsider)
    `when`(testregelDAO.getTestreglarForMaaling(id)).thenReturn(Result.success(testregelList))
    `when`(loeysingsRegisterClient.getMany(listOf(uutilsynetLoeysing.id)))
        .thenReturn(Result.success(listOf(uutilsynetLoeysing)))

    server
        .expect(
            ExpectedCount.manyTimes(),
            MockRestRequestMatchers.requestTo(
                CoreMatchers.startsWith(autoTesterClient.autoTesterProperties.url)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
        .andExpect(
            MockRestRequestMatchers.content()
                .json(objectMapper.writeValueAsString(expectedRequestData)))
        .andRespond(MockRestResponseCreators.withSuccess(jsonResponse, MediaType.APPLICATION_JSON))

    val result = maalingResource.putNewStatus(id, status)

    Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
  }
}
