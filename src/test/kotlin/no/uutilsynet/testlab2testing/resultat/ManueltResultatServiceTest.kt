package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagList
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.time.Instant

@SpringBootTest
class ManueltResultatServiceTest(@Autowired val testUtils: TestUtils) {

  private val testgrunnlagDAO = mock(TestgrunnlagDAO::class.java)
  private val testResultatDAO = mock(TestResultatDAO::class.java)
  private val sideutvalDAO = mock(SideutvalDAO::class.java)
  private val bildeService = mock(BildeService::class.java)
  private val resultatDAO = mock(ResultatDAO::class.java)
  private val kravregisterClient = mock(KravregisterClient::class.java)
  private val testregelService = mock(TestregelService::class.java)

  private val manueltResultatService =
      ManueltResultatService(
          resultatDAO,
          kravregisterClient,
          testregelService,
          testgrunnlagDAO,
          testResultatDAO,
          sideutvalDAO,
          bildeService)

  @Test
  fun `test getFilteredAndMappedResults with valid filter`() {
    val mockResult =
        listOf(
            ResultatManuellKontroll(
                1,
                1,
                1,
                1,
                1,
                Brukar("testbrukar", "testbrukar"),
                null,
                null,
                null,
                listOf(ResultatManuellKontrollBase.Svar("1", "Kommentar")),
                null,
                ResultatManuellKontrollBase.Status.Ferdig,
                "kommentar",
                Instant.now()))

      val testgrunnlagList = mock(TestgrunnlagKontroll::class.java)

      val testregel = testUtils.testregelObject()


    `when`(testgrunnlagDAO.getTestgrunnlagForKontroll(1))
        .thenReturn(TestgrunnlagList(testgrunnlagList, listOf( testgrunnlagList)))
    `when`(testResultatDAO.getManyResults(anyInt())).thenReturn(Result.success(mockResult))
    `when`(sideutvalDAO.getSideutvalUrlMapKontroll(anyList())).thenReturn(mapOf(1 to URI("https://test.com").toURL()))
      `when`(testregelService.getTestregel(anyInt())).thenReturn(testregel)

    val result = manueltResultatService.getFilteredAndMappedResults(1, 1) { it.testregelId == 1 }

    assertEquals(1, result.size)
  }

  @Test
  fun `test getTestresultatForKontroll returns filtered results`() {
    val mockTestgrunnlag = mock(TestgrunnlagList::class.java)
    `when`(mockTestgrunnlag.opprinneligTest).thenReturn(mock(TestgrunnlagKontroll::class.java))
    `when`(mockTestgrunnlag.opprinneligTest.id).thenReturn(1)
    `when`(testgrunnlagDAO.getTestgrunnlagForKontroll(1)).thenReturn(mockTestgrunnlag)
    `when`(testResultatDAO.getManyResults(1)).thenReturn(Result.success(emptyList()))

    val result = manueltResultatService.getTestresultatForKontroll(1, 1)

    assertTrue(result.isEmpty())
  }
}
