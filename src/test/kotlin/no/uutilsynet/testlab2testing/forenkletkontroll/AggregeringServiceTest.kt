package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates
import kotlin.random.Random
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalElementBase
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
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

  @MockBean lateinit var sideutvalDAO: SideutvalDAO

  @Autowired lateinit var maalingDao: MaalingDAO

  @Autowired lateinit var testregelDAO: TestregelDAO

  @Autowired lateinit var kontrollDAO: KontrollDAO

  @Autowired lateinit var utvalDAO: UtvalDAO

  private var kontrollId: Int by Delegates.notNull()
  private var utvalId: Int by Delegates.notNull()

  val testreglerSomSkalSlettes: MutableList<Int> = mutableListOf()

  @AfterAll
  fun cleanup() {
    maalingDao.jdbcTemplate.update(
        "delete from maalingv1 where navn = :namn", mapOf("namn" to "Testmaaling_aggregering"))
    testreglerSomSkalSlettes.forEach { testregelDAO.deleteTestregel(it) }

    utvalDAO.deleteUtval(utvalId)
    kontrollDAO.deleteKontroll(kontrollId)
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

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1)).thenReturn(testLoeysing)

    Mockito.`when`(kravregisterClient.getKravIdFromSuksesskritterium("1.1.1")).thenReturn(1)
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")

    aggregeringService.saveAggregertResultatTestregelAutomatisk(testKoeyring)

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
            Brukar("testar", "test"))
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

    val testregelId =
        testregelDAO.createTestregel(testregel).also { testreglerSomSkalSlettes.add(it) }

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
            testregelGjennomsnittlegSideSamsvarProsent = 1.0,
            testregelGjennomsnittlegSideBrotProsent = 1.0)

    return aggregeringTestregel
  }

  @Test
  fun updateEqualsDeleteAndInsert() {
    val testLoeysing = Loeysing(1, "test", TEST_URL, TEST_ORGNR)

    Mockito.`when`(loeysingsRegisterClient.getLoeysingFromId(1)).thenReturn(testLoeysing)

    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")

    Mockito.`when`(sideutvalDAO.getSideutvalUrlMapKontroll(listOf(1)))
        .thenReturn(mapOf(1 to URI("https://www.example.com").toURL()))

    val sakId = createTestKontroll()
    val resultatKontroll1 =
        ResultatManuellKontroll(
            1,
            sakId,
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
            sakId,
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

    val result = aggregeringService.getAggregertResultatTestregel(testgrunnlagId = sakId)
    assertThat(result).hasSize(1)
    assertThat(result[0].talElementBrot).isEqualTo(1)
    assertThat(result[0].talElementSamsvar).isEqualTo(1)

    aggregeringService.saveAggregertResultatSide(listOf(resultatKontroll1, resultatKontrol2))
    aggregeringService.saveAggregertResultatSide(listOf(resultatKontroll1, resultatKontrol2))
    val result2 = aggregeringService.getAggregertResultatSide(testgrunnlagId = sakId)
    assertThat(result2).hasSize(1)
    assertThat(result2[0].talElementBrot).isEqualTo(1)
    assertThat(result2[0].talElementSamsvar).isEqualTo(1)

    aggregeringService.saveAggregertResultatSuksesskriterium(
        listOf(resultatKontroll1, resultatKontrol2))
    aggregeringService.saveAggregertResultatSuksesskriterium(
        listOf(resultatKontroll1, resultatKontrol2))

    val result3 = aggregeringService.getAggregertResultatSuksesskriterium(testgrunnlagId = sakId)

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
    val testresultat: ArrayList<ResultatManuellKontroll> = ArrayList<ResultatManuellKontroll>()
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

  private fun createTestKontroll(): Int {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Sakstype.Arkivsak,
            "1234",
            Kontrolltype.InngaaendeKontroll)

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontrolltype.InngaaendeKontroll,
            opprettKontroll.tittel,
            opprettKontroll.saksbehandler,
            opprettKontroll.sakstype,
            opprettKontroll.arkivreferanse,
        )

    /* Add utval */
    val loeysingId = 1
    utvalId = utvalDAO.createUtval("test-skal-slettes", listOf(loeysingId)).getOrThrow()
    kontrollDAO.updateKontroll(kontroll, utvalId)

    /* Add testreglar */
    val testregel = testregelDAO.getTestregelList().first()
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregel.id))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalElementBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    return kontrollId
  }
}
