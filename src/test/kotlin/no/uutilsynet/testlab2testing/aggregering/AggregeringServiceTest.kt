package no.uutilsynet.testlab2testing.aggregering

import java.net.URI
import java.time.Instant
import kotlin.random.Random
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.OpprettTestgrunnlag
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestKoeyring
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container

private val TEST_URL = URI("http://localhost:8080/").toURL()

private const val TEST_ORGNR = "123456789"

private const val TEST_ORG = "Test AS"

@SpringBootTest(
    properties = ["spring.datasource.url = jdbc:tc:postgresql:16-alpine:///AggregeringServiceTest"])
class AggregeringServiceTest(
    @Autowired val aggregeringService: AggregeringService,
    @Autowired val testUtils: TestUtils,
) {

  @MockitoSpyBean lateinit var testgrunnlagService: TestgrunnlagService
  @MockitoBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockitoBean lateinit var kravregisterClient: KravregisterClient

  @MockitoBean lateinit var autoTesterClient: AutoTesterClient

  @MockitoBean lateinit var sideutvalDAO: SideutvalDAO

  @MockitoSpyBean lateinit var maalingDao: MaalingDAO

  @MockitoBean lateinit var testregelCache: TestregelCache

  companion object {
    @Container
    @JvmStatic
    var postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15.3")
  }

  @Test
  fun saveAggregeringTestregel() {

    val aggregeringTestregel = createTestMaaling()
    val maalingId = aggregeringTestregel.maalingId as Int

    val testLoeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR, TEST_ORG)

    val testKoeyring: TestKoeyring.Ferdig = setupTestKoeyring(testLoeysing)

    val testregel = testUtils.createTestregelAggregate()

    Mockito.`when`(
            autoTesterClient.fetchResultatAggregering(
                TEST_URL.toURI(), AutoTesterClient.ResultatUrls.urlAggreggeringTR))
        .thenReturn(listOf(aggregeringTestregel))

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1)).thenReturn(testLoeysing)

    Mockito.`when`(kravregisterClient.getKravIdFromSuksesskritterium("1.1.1")).thenReturn(1)
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")
    Mockito.`when`(kravregisterClient.listKrav()).thenReturn(listOf(testUtils.kravWcag2xObject()))
    Mockito.doReturn(listOf(testLoeysing)).`when`(maalingDao).getLoeysingarForMaaling(maalingId)
    Mockito.`when`(testregelCache.getTestregelByKey(anyString())).thenReturn(testregel)
    Mockito.`when`(testregelCache.getTestregelById(anyInt())).thenReturn(testregel)

    aggregeringService.saveAggregertResultatTestregelAutomatisk(testKoeyring)

    val retrievedAggregering =
        maalingId.let { aggregeringService.getAggregertResultatTestregel(it) }

    assertThat(retrievedAggregering).isNotEmpty
    assert(retrievedAggregering[0].maalingId == maalingId)
    assert(retrievedAggregering[0].testregelId == aggregeringTestregel.testregelId)
    assert(retrievedAggregering[0].suksesskriterium == aggregeringTestregel.suksesskriterium)
  }

  private fun setupTestKoeyring(testLoeysing: Loeysing): TestKoeyring.Ferdig {
    val testKoeyring: TestKoeyring.Ferdig =
        TestKoeyring.Ferdig(
            loeysing = testLoeysing,
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
            Brukar("testar", "test"),
            10)
    return testKoeyring
  }

  fun createTestMaaling(): AggregertResultatTestregel {
    val crawlParameters = CrawlParameters(10, 10)

    val testregelInit = testUtils.testregelInitObject()

    val testregelId = testUtils.createTestregelKrav().id

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
            loeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR, TEST_ORG),
            testregelId = testregelInit.testregelId,
            suksesskriterium = "1.1.1",
            fleireSuksesskriterium = listOf("1.1.1", "1.1.1"),
            talElementSamsvar = 1,
            talElementBrot = 2,
            talElementVarsel = 1,
            talElementIkkjeForekomst = 1,
            talSiderSamsvar = 1,
            talSiderBrot = 1,
            talSiderIkkjeForekomst = 1,
            testregelGjennomsnittlegSideSamsvarProsent = 1.0,
            testregelGjennomsnittlegSideBrotProsent = 1.0)

    return aggregeringTestregel
  }

  @Test
  fun updateEqualsDeleteAndInsert() {
    val testLoeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR, TEST_ORG)

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1)).thenReturn(testLoeysing)

    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")

    Mockito.`when`(sideutvalDAO.getSideutvalUrlMapKontroll(listOf(1)))
        .thenReturn(mapOf(1 to URI("https://www.example.com").toURL()))

    Mockito.doReturn(listOf(testLoeysing)).`when`(maalingDao).getLoeysingarForMaaling(anyInt())
    Mockito.doReturn(listOf(testLoeysing))
        .`when`(testgrunnlagService)
        .getLoeysingForTestgrunnlag(anyInt())
    val testregel = testUtils.createTestregelAggregate()

    Mockito.`when`(testregelCache.getTestregelByKey(anyString())).thenReturn(testregel)
    Mockito.`when`(testregelCache.getTestregelById(anyInt())).thenReturn(testregel)

    val kontroll =
        testUtils.createKontroll(
            "Kontroll", Kontrolltype.InngaaendeKontroll, listOf(1), testregel.id)
    val testgrunnlagbase = OpprettTestgrunnlag("Testgrunnlag", TestgrunnlagType.OPPRINNELEG_TEST)
    val testgrunnlagId = testUtils.createTestgrunnlag(testgrunnlagbase, kontroll)

    val resultatKontroll1 =
        ResultatManuellKontroll(
            1,
            testgrunnlagId,
            loeysingId = 1,
            testregelId = 1,
            sideutvalId = 1,
            brukar = Brukar("testar", "test"),
            elementOmtale = "Hovedoverskrift",
            elementResultat = TestresultatUtfall.brot,
            elementUtfall = "Feil",
            svar = listOf(ResultatManuellKontrollBase.Svar("Steg 1", "Svar 1")),
            testVartUtfoert = Instant.now(),
            status = ResultatManuellKontrollBase.Status.Ferdig,
            kommentar = "Kommentar",
            sistLagra = Instant.now())

    val resultatKontrol2 =
        ResultatManuellKontroll(
            1,
            testgrunnlagId,
            loeysingId = 1,
            testregelId = 1,
            sideutvalId = 1,
            brukar = Brukar("testar", "test"),
            elementOmtale = "Bilde",
            elementResultat = TestresultatUtfall.samsvar,
            elementUtfall = "Heilt ok",
            svar = listOf(ResultatManuellKontrollBase.Svar("Steg 1", "Svar 1")),
            testVartUtfoert = Instant.now(),
            status = ResultatManuellKontrollBase.Status.Ferdig,
            kommentar = "Kommentar",
            sistLagra = Instant.now())

    val status =
        aggregeringService.saveAggregertResultatTestregel(
            listOf(resultatKontroll1, resultatKontrol2))
    assertThat(status.isSuccess).isEqualTo(true)

    val status2 =
        aggregeringService.saveAggregertResultatTestregel(
            listOf(resultatKontroll1, resultatKontrol2))
    assertThat(status2.isSuccess).isEqualTo(true)

    val result = aggregeringService.getAggregertResultatTestregel(testgrunnlagId = testgrunnlagId)
    assertThat(result).hasSize(1)
    assertThat(result[0].talElementBrot).isEqualTo(1)
    assertThat(result[0].talElementSamsvar).isEqualTo(1)

    aggregeringService.saveAggregertResultatSide(listOf(resultatKontroll1, resultatKontrol2))
    aggregeringService.saveAggregertResultatSide(listOf(resultatKontroll1, resultatKontrol2))
    val result2 = aggregeringService.getAggregertResultatSide(testgrunnlagId = testgrunnlagId)
    assertThat(result2).hasSize(1)
    assertThat(result2[0].talElementBrot).isEqualTo(1)
    assertThat(result2[0].talElementSamsvar).isEqualTo(1)

    aggregeringService.saveAggregertResultatSuksesskriterium(
        listOf(resultatKontroll1, resultatKontrol2))
    aggregeringService.saveAggregertResultatSuksesskriterium(
        listOf(resultatKontroll1, resultatKontrol2))

    val result3 =
        aggregeringService.getAggregertResultatSuksesskriterium(testgrunnlagId = testgrunnlagId)

    assertThat(result3).hasSize(1)
    assertThat(result3[0].talSiderBrot).isEqualTo(1)
    assertThat(result3[0].talSiderSamsvar).isEqualTo(0)
  }

  @Test
  fun calculateTestregelGjennomsnitt() {
    val testresultat: ArrayList<ResultatManuellKontroll> = resultatManuellKontrollTestdata()

    val gjennomsnittTestresultat = aggregeringService.calculateTestregelGjennomsnitt(testresultat)

    assertThat(
            gjennomsnittTestresultat.testregelGjennomsnittlegSideSamsvarProsent!! +
                gjennomsnittTestresultat.testregelGjennomsnittlegSideBrotProsent!!)
        .isCloseTo(1.0, Offset.offset(0.00001))
  }

  @Test
  fun resultatPrTestregelPrSide() {
    val testresultat: ArrayList<ResultatManuellKontroll> = resultatManuellKontrollTestdata()

    testresultat
        .groupBy { it.sideutvalId }
        .forEach { _ ->
          val result = aggregeringService.processPrSideutval(testresultat)
          assertThat(result.brotprosentTrSide + result.samsvarsprosentTrSide)
              .isCloseTo(1.0, Offset.offset(0.00001))
          if (result.ikkjeForekomst) {
            assertThat(result.brotprosentTrSide).isCloseTo(0.0, Offset.offset(0.00001))
            assertThat(result.samsvarsprosentTrSide).isCloseTo(0.0, Offset.offset(0.00001))
          }
        }
  }

  private fun resultatManuellKontrollTestdata(): ArrayList<ResultatManuellKontroll> {
    val testresultat: ArrayList<ResultatManuellKontroll> = ArrayList()
    val utfall: Map<Int, TestresultatUtfall> =
        mapOf(
            1 to TestresultatUtfall.brot,
            2 to TestresultatUtfall.samsvar,
            3 to TestresultatUtfall.ikkjeForekomst)
    var id = 1
    for (side in 1..10) {
      for (testregel in 1..10) {
        val elementResultat = utfall[Random.nextInt(1, 3)]
        testresultat.add(
            ResultatManuellKontroll(
                id,
                1,
                loeysingId = 1,
                testregelId = testregel,
                side,
                brukar = Brukar("testar", "test"),
                elementOmtale = "Hovedoverskrift",
                elementResultat = elementResultat,
                elementUtfall = "elementUtfall",
                svar = listOf(ResultatManuellKontrollBase.Svar("Steg 1", "Svar 1")),
                testVartUtfoert = Instant.now(),
                status = ResultatManuellKontrollBase.Status.Ferdig,
                kommentar = "Kommentar",
                sistLagra = Instant.now()))
        id++
      }
    }
    return testresultat
  }
}
