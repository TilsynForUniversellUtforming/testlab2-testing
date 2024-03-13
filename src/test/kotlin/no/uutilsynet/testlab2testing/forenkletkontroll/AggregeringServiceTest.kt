package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import no.uutilsynet.testlab2testing.testregel.TestregelInnholdstype
import no.uutilsynet.testlab2testing.testregel.TestregelModus
import no.uutilsynet.testlab2testing.testregel.TestregelStatus
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

private val TEST_URL = URI("http://localhost:8080/").toURL()

private const val TEST_ORGNR = "123456789"

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AggregeringServiceTest(@Autowired val aggregeringService: AggregeringService) {

  @MockBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockBean lateinit var kravregisterClient: KravregisterClient

  @MockBean lateinit var autoTesterClient: AutoTesterClient

  @Autowired lateinit var maalingDao: MaalingDAO

  @Autowired lateinit var testregelDAO: TestregelDAO

  @AfterAll
  fun cleanup() {
    maalingDao.jdbcTemplate.update(
        "delete from maalingv1 where navn = :namn", mapOf("namn" to "Testmaaling_aggregering"))
    testregelDAO.jdbcTemplate.update(
        "delete from testregel where namn = :namn", mapOf("namn" to name))
  }

  @Test
  fun saveAggregeringTestregel() {

    val aggregeringTestregel = createTestMaaling()
    val maalingId = aggregeringTestregel.maalingId

    val testLoeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR)

    val testKoeyring: TestKoeyring.Ferdig = setupTestKoeyring(testLoeysing)

    Mockito.`when`(
            autoTesterClient.fetchResultatAggregering(
                TEST_URL.toURI(), AutoTesterClient.ResultatUrls.urlAggreggeringTR))
        .thenReturn(listOf(aggregeringTestregel))

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1))
        .thenReturn(Result.success(testLoeysing))

    Mockito.`when`(kravregisterClient.getKravIdFromSuksesskritterium("1.1.1"))
        .thenReturn(Result.success(1))
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1))
        .thenReturn(Result.success("1.1.1"))

    aggregeringService.saveAggregertResultat(testKoeyring)

    val retrievedAggregering =
        maalingId?.let { aggregeringService.getAggregertResultatTestregel(it) }

    assertThat(retrievedAggregering).isNotEmpty
    assert(retrievedAggregering?.get(0)?.maalingId == maalingId)
    assert(retrievedAggregering?.get(0)?.testregelId == aggregeringTestregel.testregelId)
    assert(retrievedAggregering?.get(0)?.suksesskriterium == aggregeringTestregel.suksesskriterium)
  }

  private fun setupTestKoeyring(testLoeysing: Loeysing): TestKoeyring.Ferdig {
    val testKoeyring: TestKoeyring.Ferdig =
        TestKoeyring.Ferdig(
            crawlResultat =
                CrawlResultat.Ferdig(
                    antallNettsider = 1,
                    statusUrl = TEST_URL,
                    loeysing = testLoeysing,
                    sistOppdatert = Instant.now(),
                    nettsider = emptyList()),
            sistOppdatert = Instant.now(),
            statusURL = TEST_URL,
            lenker =
                AutoTesterClient.AutoTesterLenker(
                    urlFulltResultat = TEST_URL,
                    urlBrot = TEST_URL,
                    urlAggregeringSideTR = TEST_URL,
                    urlAggregeringTR = TEST_URL,
                    urlAggregeringSide = TEST_URL,
                    urlAggregeringLoeysing = TEST_URL,
                    urlAggregeringSK = TEST_URL),
        )
    return testKoeyring
  }

  fun createTestMaaling(): AggregertResultatTestregel {
    val crawlParameters = CrawlParameters(10, 10)
    val testregelNoekkel = RandomStringUtils.randomAlphanumeric(5)

    val testregel =
        TestregelInit(
            testregelId = testregelNoekkel,
            namn = name,
            kravId = 1,
            status = TestregelStatus.publisert,
            type = TestregelInnholdstype.nett,
            modus = TestregelModus.automatisk,
            spraak = TestlabLocale.nb,
            testregelSchema = testregelNoekkel,
            innhaldstypeTesting = 1,
            tema = 1,
            testobjekt = 1,
            kravTilSamsvar = "")

    val testregelId = testregelDAO.createTestregel(testregel)

    val maalingId =
        maalingDao.createMaaling(
            "Testmaaling_aggregering",
            Instant.now(),
            listOf(1),
            listOf(testregelId),
            crawlParameters)

    val aggregeringTestregel =
        AggregertResultatTestregel(
            maalingId = maalingId,
            loeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR),
            testregelId = testregelNoekkel,
            suksesskriterium = "1.1.1",
            fleireSuksesskriterium = listOf("1.1.1", "1.1.1"),
            talElementSamsvar = 1,
            talElementBrot = 2,
            talElementVarsel = 1,
            talElementIkkjeForekomst = 1,
            talSiderSamsvar = 1,
            talSiderBrot = 1,
            talSiderIkkjeForekomst = 1,
            testregelGjennomsnittlegSideSamsvarProsent = 1.0f,
            testregelGjennomsnittlegSideBrotProsent = 1.0f)

    return aggregeringTestregel
  }
}
