package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.forenkletkontroll.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.forenkletkontroll.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.*
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

private const val TEST_URL = "http://localhost:8080/"

private const val TEST_ORGNR = "123456789"

@SpringBootTest
class AggregeringServiceTest(@Autowired val aggregeringService: AggregeringService) {

  @MockBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockBean lateinit var kravregisterClient: KravregisterClient

  @MockBean lateinit var autoTesterClient: AutoTesterClient

  @Autowired lateinit var maalingDao: MaalingDAO

  @Autowired lateinit var testregelDAO: TestregelDAO

  @Test
  fun saveAggregeringTestregel() {

    val aggregeringTestregel = createTestMaaling()
    val maalingId = aggregeringTestregel.maalingId

    val testLoeysing = Loeysing(1, "test", URL(TEST_URL), TEST_ORGNR)

    val testKoeyring: TestKoeyring.Ferdig = setupTestKoeyring(testLoeysing)

    Mockito.`when`(
            autoTesterClient.fetchResultatAggregering(
                URL(TEST_URL).toURI(), AutoTesterClient.ResultatUrls.urlAggreggeringTR))
        .thenReturn(listOf(aggregeringTestregel))

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1))
        .thenReturn(Result.success(testLoeysing))

    Mockito.`when`(kravregisterClient.getKravIdFromSuksesskritterium("1.1.1"))
        .thenReturn(Result.success(1))
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1))
        .thenReturn(Result.success("1.1.1"))

    aggregeringService.saveAggregertResultatTestregel(testKoeyring)

    val retrievedAggregering = aggregeringService.getAggregertResultatTestregel(maalingId)

    assertThat(retrievedAggregering).isNotEmpty
    assert(retrievedAggregering[0].maalingId == maalingId)
    assert(retrievedAggregering[0].testregelId == aggregeringTestregel.testregelId)
    assert(retrievedAggregering[0].suksesskriterium == aggregeringTestregel.suksesskriterium)
  }

  private fun setupTestKoeyring(testLoeysing: Loeysing): TestKoeyring.Ferdig {
    val testKoeyring: TestKoeyring.Ferdig =
        TestKoeyring.Ferdig(
            crawlResultat =
                CrawlResultat.Ferdig(
                    antallNettsider = 1,
                    statusUrl = URI(TEST_URL).toURL(),
                    loeysing = testLoeysing,
                    sistOppdatert = Instant.now(),
                    nettsider = emptyList()),
            sistOppdatert = Instant.now(),
            statusURL = URI(TEST_URL).toURL(),
            lenker =
                AutoTesterClient.AutoTesterLenker(
                    urlFulltResultat = URI(TEST_URL).toURL(),
                    urlBrot = URI(TEST_URL).toURL(),
                    urlAggregeringSideTR = URI(TEST_URL).toURL(),
                    urlAggregeringTR = URI(TEST_URL).toURL(),
                    urlAggregeringSide = URI(TEST_URL).toURL(),
                    urlAggregeringLoeysing = URI(TEST_URL).toURL(),
                    urlAggregeringSK = URI(TEST_URL).toURL()),
        )
    return testKoeyring
  }

  fun createTestMaaling(): AggregertResultatTestregel {
    val crawlParameters = CrawlParameters(10, 10)
    val testregelNoekkel = RandomStringUtils.randomAlphanumeric(5)

    val testregel =
        TestregelInitAutomatisk(testregelNoekkel, "QW-1", "1.1.1", 1, 1, testregelNoekkel, 1)

    val testregelId = testregelDAO.createAutomatiskTestregel(testregel)

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
            loeysing = Loeysing(1, "test", URI(TEST_URL).toURL(), TEST_ORGNR),
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
