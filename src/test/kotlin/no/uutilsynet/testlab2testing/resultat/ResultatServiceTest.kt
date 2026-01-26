package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.common.SortOrder
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.common.SortParamTestregel
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.loeysing.Verksemd
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates

@SpringBootTest(
    properties =
        arrayOf("spring.datasource.url: jdbc:tc:postgresql:16-alpine:///ResultatServiceTest"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultatServiceTest(
    @Autowired val aggregeringDAO: AggregeringDAO,
    @Autowired val maalingDao: MaalingDAO,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val resultatService: ResultatService,
    @Autowired val testUtils: TestUtils
) {

  private var kontrollId: Int by Delegates.notNull()
  private var utvalId: Int by Delegates.notNull()
  private var maalingId: Int by Delegates.notNull()
  private var testregelId: Int by Delegates.notNull()
  private var aggregertResultat: AggregeringPerTestregelDB by Delegates.notNull()

  @MockitoBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient
  @MockitoBean lateinit var testgrunnlagDAO: TestgrunnlagDAO
  @MockitoBean lateinit var testResultatDAO: TestResultatDAO
  @MockitoBean lateinit var sideutvalDAO: SideutvalDAO
  @MockitoBean lateinit var kravregisterClient: KravregisterClient
  @MockitoBean lateinit var testregelCache: TestregelCache
  @MockitoSpyBean lateinit var kontrollDAO: KontrollDAO

  @Test
  fun getTestresultatMaaling() {
    kontrollId = createTestKontroll()
    createTestMaaling()
    val testloeysing =
        Loeysing.Expanded(
            1,
            "testloeysing",
            URI.create("https://www.uutilsynet.no").toURL(),
            Verksemd(1, "Testverksemd", "123456789"))
    Mockito.`when`(loeysingsRegisterClient.getManyExpanded(Mockito.anyList()))
        .thenReturn(Result.success(listOf(testloeysing)))
    val resultat =
        resultatService.getResultatList(Kontrolltype.ForenklaKontroll).filter {
          it.id == kontrollId
        }

    assert(resultat.isNotEmpty())

    val resultatKontroll = resultat.first()

    assertEquals(resultatKontroll.id, kontrollId)
    assertEquals(resultatKontroll.loeysingar.size, 1)
    assertEquals(resultatKontroll.loeysingar[0].score, 0.5)

    utvalDAO.deleteUtval(utvalId)
    kontrollDAO.deleteKontroll(kontrollId)
    maalingDao.deleteMaaling(maalingId)
  }

  fun createTestMaaling(): Int {
    val crawlParameters = CrawlParameters(10, 10)

    maalingId =
        maalingDao.createMaaling(
            "Testmaaling_resultat", Instant.now(), listOf(1), listOf(testregelId), crawlParameters)

    aggregertResultat =
        AggregeringPerTestregelDB(
            maalingId, 1, testregelId, 1, arrayListOf(1, 2), 1, 2, 1, 1, 1, 1, 1, 0.5, 0.5, null)

    aggregeringDAO.createAggregertResultatTestregel(aggregertResultat)
    maalingDao.updateKontrollId(kontrollId, maalingId)

    return maalingId
  }

  private fun createTestKontroll(): Int {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Sakstype.Arkivsak,
            "1234",
            Kontrolltype.ForenklaKontroll)

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    val kontroll =
        Kontroll(
            kontrollId,
            Kontrolltype.ForenklaKontroll,
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

    testregelId = testUtils.createTestregel()
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregelId))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    return kontrollId
  }


  @Test
  fun uferdigResultatFiltrert() {
    val testgrunnlagKontroll =
        TestgrunnlagKontroll(
            1,
            1,
            "test",
            emptyList(),
            emptyList(),
            TestgrunnlagType.OPPRINNELEG_TEST,
            Instant.now())
    val testgrunnlagList = TestgrunnlagList(testgrunnlagKontroll, emptyList())
    val testregel = testUtils.testregelKravObject()

    val resultat1 =
        ResultatManuellKontroll(
            1,
            1,
            1,
            1,
            1,
            Brukar("testar", "testar"),
            "Ein knapp",
            TestresultatUtfall.samsvar,
            "Alt ok",
            emptyList(),
            Instant.now(),
            ResultatManuellKontrollBase.Status.UnderArbeid,
            null,
            Instant.now())
    val resultat2 =
        ResultatManuellKontroll(
            1,
            1,
            1,
            1,
            1,
            Brukar("testar", "testar"),
            "Eit bilde",
            null,
            null,
            emptyList(),
            Instant.now(),
            ResultatManuellKontrollBase.Status.UnderArbeid,
            null,
            Instant.now())

    val sideUtvalList = mapOf(1 to URI.create("https://www.example.com").toURL())

    val resultatliste = listOf(resultat1, resultat2)

    Mockito.`when`(testgrunnlagDAO.getTestgrunnlagForKontroll(1)).thenReturn(testgrunnlagList)

    Mockito.`when`(testResultatDAO.getManyResults(1)).thenReturn(Result.success(resultatliste))

    Mockito.`when`(sideutvalDAO.getSideutvalUrlMapKontroll(listOf(1))).thenReturn(sideUtvalList)

    Mockito.`when`(testregelCache.getTestregelById(testregel.id)).thenReturn(testregel)
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")
    Mockito.`when`(kravregisterClient.listKrav()).thenReturn(listOf(testUtils.kravWcag2xObject()))
    Mockito.doReturn(Kontrolltype.InngaaendeKontroll).`when`(kontrollDAO).getKontrollType(1)

    val sortPaginationParams =
        SortPaginationParams(
            sortParam = SortParamTestregel.side,
            sortOrder = SortOrder.asc,
            pageNumber = 0,
            pageSize = 20)

    val resultat = resultatService.getTestresultatDetaljerPrTestregel(1, 1, 1, sortPaginationParams)
    assertNotNull(resultat)

    assertTrue(resultat.isNotEmpty())

    assertTrue(resultat.size == 1)
  }
}
