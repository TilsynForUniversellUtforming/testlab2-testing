package no.uutilsynet.testlab2testing.forenkletkontroll

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

    val testLoeysing = Loeysing(1, "test", URL("http://localhost:8080/"), "123456789")

    val testKoeyring: TestKoeyring.Ferdig = setupTestKoeyring(testLoeysing)

    Mockito.`when`(
            autoTesterClient.fetchResultatAggregering(
                URL("http://localhost:8080/").toURI(),
                AutoTesterClient.ResultatUrls.urlAggreggeringTR))
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
                    statusUrl = URL("http://localhost:8080/"),
                    loeysing = testLoeysing,
                    sistOppdatert = Instant.now(),
                    nettsider = emptyList()),
            sistOppdatert = Instant.now(),
            statusURL = URL("http://localhost:8080/"),
            lenker =
                AutoTesterClient.AutoTesterLenker(
                    urlFulltResultat = URL("http://localhost:8080/"),
                    urlBrot = URL("http://localhost:8080/"),
                    urlAggregeringSideTR = URL("http://localhost:8080/"),
                    urlAggregeringTR = URL("http://localhost:8080/"),
                    urlAggregeringSide = URL("http://localhost:8080/"),
                    urlAggregeringLoeysing = URL("http://localhost:8080/"),
                    urlAggregeringSK = URL("http://localhost:8080/")),
        )
    return testKoeyring
  }

  fun createTestMaaling(): AggregertResultatTestregel {
    val crawlParameters = CrawlParameters(10, 10)
    val testregelNoekkel = RandomStringUtils.randomAlphanumeric(5)

    val testregel =
        TestregelInitAutomatisk(testregelNoekkel, "QW-1", "1.1.1", 1, 1, testregelNoekkel)

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
            loeysing = Loeysing(1, "test", URL("http://localhost:8080/"), "123456789"),
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
