package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Loeysingstype
import no.uutilsynet.testlab2.constants.Reaksjonstype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase.Status
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.Sideutval
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataListElement
import no.uutilsynet.testlab2testing.styringsdata.StyringsdataService
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestoverviewServiceTest(@Autowired val testoverviewService: TestoverviewService) {

  @MockitoBean private lateinit var testgrunnlagService: TestgrunnlagService
  @MockitoBean private lateinit var loeysingsRegisterClient: LoeysingsRegisterClient
  @MockitoBean private lateinit var statisticsService: TestOverviewStatisticsService
  @MockitoBean private lateinit var styringsdataService: StyringsdataService
  @MockitoBean private lateinit var testregelClient: TestregelClient
  @MockitoBean private lateinit var testResultatDAO: TestResultatDAO
  @MockitoBean private lateinit var kontrollDAO: KontrollDAO

  private val loeysingId = 1
  private val kontrollId = 10
  private val testgrunnlagId = 100

  private fun loeysing() =
      Loeysing(
          id = loeysingId,
          namn = "Test Loeysing",
          url = URI("https://example.com").toURL(),
          orgnummer = "123456789",
          verksemdNamn = "Test Verksemd")

  private fun sideutval(id: Int = 1) =
      Sideutval(
          id = id,
          loeysingId = loeysingId,
          typeId = 1,
          begrunnelse = "Begrunnelse",
          url = URI.create("https://example.com/side"),
          egendefinertType = null)

  private fun testgrunnlag(
      id: Int = testgrunnlagId,
      sideutval: List<Sideutval> = listOf(sideutval()),
      type: TestgrunnlagType = TestgrunnlagType.OPPRINNELEG_TEST
  ) =
      TestgrunnlagKontroll(
          id = id,
          kontrollId = kontrollId,
          namn = "Test Testgrunnlag",
          testreglar = listOf(1, 2),
          sideutval = sideutval,
          type = type,
          datoOppretta = Instant.now())

  private fun kontrollDB() =
      KontrollDAO.KontrollDB(
          id = kontrollId,
          tittel = "Test Kontroll",
          saksbehandler = "saksbehandler",
          sakstype = "TILSYN",
          arkivreferanse = "ref",
          kontrolltype = Kontrolltype.InngaaendeKontroll,
          utval = null,
          testreglar = null,
          sideutval = emptyList(),
          opprettaDato = Instant.now(),
          styringsdataId = null)

  private fun styringsdataElement(
      loeysingId: Int = this.loeysingId,
      paaleggReaksjon: Reaksjonstype = Reaksjonstype.ingenReaksjon,
      botReaksjon: Reaksjonstype = Reaksjonstype.ingenReaksjon
  ) =
      StyringsdataListElement(
          id = 1,
          kontrollId = kontrollId,
          ansvarleg = "ansvarleg",
          oppretta = LocalDate.now(),
          frist = LocalDate.now().plusDays(30),
          sistLagra = Instant.now(),
          loeysingId = loeysingId,
          reaksjon = Reaksjonstype.ingenReaksjon,
          paaleggReaksjon = paaleggReaksjon,
          paaleggKlageReaksjon = Reaksjonstype.ingenReaksjon,
          botReaksjon = botReaksjon,
          botKlageReaksjon = Reaksjonstype.ingenReaksjon,
          paaleggId = null,
          paaleggKlageId = null,
          botId = null,
          botKlageId = null)

  private fun testStatusCount() =
      TestStatusCount(
          loeysingId = loeysingId,
          testgrunnlagId = testgrunnlagId,
          total = 2,
          ferdig = 0,
          underArbeid = 0,
          ikkjeStarta = 2,
          percentagePerSide = 0.0,
          persentagePerInnholdstype = 0.0)

  private fun resultat(status: Status = Status.IkkjePaabegynt) =
      ResultatManuellKontroll(
          id = 1,
          testgrunnlagId = testgrunnlagId,
          loeysingId = loeysingId,
          testregelId = 1,
          sideutvalId = 1,
          elementOmtale = null,
          elementResultat = null,
          elementUtfall = null,
          svar = emptyList(),
          testVartUtfoert = null,
          status = status,
          kommentar = null,
          sistLagra = Instant.now())

  // --- TestgrunnlagList.toList() ---

  @Test
  fun `toList returns opprinneligTest and restestar as flat list`() {
    val opprinnelig = testgrunnlag(id = 1)
    val retest1 = testgrunnlag(id = 2, type = TestgrunnlagType.RETEST)
    val retest2 = testgrunnlag(id = 3, type = TestgrunnlagType.RETEST)
    val testgrunnlagList = TestgrunnlagList(opprinnelig, listOf(retest1, retest2))

    with(testoverviewService) {
      val result = testgrunnlagList.toList()
      assertEquals(3, result.size)
      assertEquals(opprinnelig, result[0])
      assertEquals(retest1, result[1])
      assertEquals(retest2, result[2])
    }
  }

  @Test
  fun `toList with no restestar returns only opprinneligTest`() {
    val opprinnelig = testgrunnlag(id = 1)
    val testgrunnlagList = TestgrunnlagList(opprinnelig, emptyList())

    with(testoverviewService) {
      val result = testgrunnlagList.toList()
      assertEquals(1, result.size)
      assertEquals(opprinnelig, result[0])
    }
  }

  // --- listTestOverviewElements ---

  @Test
  fun `listTestOverviewElements returns TestingStatus for each loeysing`() {
    val tg = testgrunnlag()
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(1, result.size)
    assertEquals(loeysingId, result[0].loeysingId)
    assertEquals("Test Loeysing", result[0].loeysingNamn)
    assertEquals(Loeysingstype.NETT, result[0].loeysingstype)
    assertEquals(Kontrolltype.InngaaendeKontroll, result[0].kontrollType)
    assertEquals(TestgrunnlagType.OPPRINNELEG_TEST, result[0].testgrunnlagType)
    assertEquals(ManuellTestStatus.IKKJE_STARTA, result[0].status)
    assertEquals(StyringsdataStatus.INGEN_REAKSJON_BRUKT, result[0].styringdataStatus)
  }

  @Test
  fun `listTestOverviewElements returns empty list when no loeysingar in sideutval`() {
    val tg = testgrunnlag(sideutval = emptyList())
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(emptyList())).thenReturn(Result.success(emptyList()))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertTrue(result.isEmpty())
  }

  // --- getTeststatus (via listTestOverviewElements) ---

  @Test
  fun `status is FERDIG when all results are Ferdig`() {
    val tg = testgrunnlag()
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())
    val ferdigResultat = listOf(resultat(Status.Ferdig))

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId))
        .thenReturn(Result.success(ferdigResultat))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, ferdigResultat, listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(ManuellTestStatus.FERDIG, result.single().status)
  }

  @Test
  fun `status is UNDER_ARBEID when results are mixed`() {
    val tg = testgrunnlag()
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())
    val mixedResultat = listOf(resultat(Status.Ferdig), resultat(Status.IkkjePaabegynt))

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(mixedResultat))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, mixedResultat, listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(ManuellTestStatus.UNDER_ARBEID, result.single().status)
  }

  // --- styringsdataStatus (via listTestOverviewElements) ---

  @Test
  fun `styringdataStatus is BOT when botReaksjon is reaksjon`() {
    val tg = testgrunnlag()
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement(botReaksjon = Reaksjonstype.reaksjon))

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(StyringsdataStatus.BOT, result.single().styringdataStatus)
  }

  @Test
  fun `styringdataStatus is PAALEG when paaleggReaksjon is reaksjon`() {
    val tg = testgrunnlag()
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement(paaleggReaksjon = Reaksjonstype.reaksjon))

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(StyringsdataStatus.PAALEG, result.single().styringdataStatus)
  }

  // --- kanSlette ---

  @Test
  fun `kanSlette is false for OPPRINNELEG_TEST even with empty resultat`() {
    val tg = testgrunnlag(type = TestgrunnlagType.OPPRINNELEG_TEST)
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(false, result.single().kanSlette)
  }

  @Test
  fun `kanSlette is true for RETEST with empty resultat`() {
    val tg = testgrunnlag(type = TestgrunnlagType.RETEST)
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(true, result.single().kanSlette)
  }

  @Test
  fun `kanSlette is false for RETEST with existing resultat`() {
    val tg = testgrunnlag(type = TestgrunnlagType.RETEST)
    val testgrunnlagList = TestgrunnlagList(tg, emptyList())
    val existingResultat = listOf(resultat())

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId))
        .thenReturn(Result.success(existingResultat))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, existingResultat, listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(false, result.single().kanSlette)
  }

  // --- multiple testgrunnlag ---

  @Test
  fun `listTestOverviewElements aggregates results across opprinneligTest and restestar`() {
    val opprinnelig = testgrunnlag(id = testgrunnlagId)
    val retestId = 200
    val retest = testgrunnlag(id = retestId, type = TestgrunnlagType.RETEST)
    val testgrunnlagList = TestgrunnlagList(opprinnelig, listOf(retest))

    `when`(kontrollDAO.getKontroller(listOf(kontrollId)))
        .thenReturn(Result.success(listOf(kontrollDB())))
    `when`(testgrunnlagService.getTestgrunnlagForKontroll(kontrollId)).thenReturn(testgrunnlagList)
    `when`(loeysingsRegisterClient.getMany(listOf(loeysingId, loeysingId)))
        .thenReturn(Result.success(listOf(loeysing())))
    `when`(testResultatDAO.getManyResults(testgrunnlagId)).thenReturn(Result.success(emptyList()))
    `when`(testResultatDAO.getManyResults(retestId)).thenReturn(Result.success(emptyList()))
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, testgrunnlagId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount())
    `when`(
            statisticsService.getTestingStatusForLoeysing(
                loeysingId, retestId, emptyList(), listOf(1, 2), listOf(1)))
        .thenReturn(testStatusCount().copy(testgrunnlagId = retestId))
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, testgrunnlagId))
        .thenReturn(styringsdataElement())
    `when`(styringsdataService.getStyringsdataForLoeysing(loeysingId, retestId))
        .thenReturn(styringsdataElement())

    val result = testoverviewService.listTestOverviewElements(kontrollId)

    assertEquals(2, result.size)
  }
}
