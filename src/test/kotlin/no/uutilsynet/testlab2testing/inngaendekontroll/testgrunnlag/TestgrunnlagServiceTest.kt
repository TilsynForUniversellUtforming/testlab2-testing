package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
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
class TestgrunnlagServiceTest(@Autowired val testgrunnlagService: TestgrunnlagService) {

  @MockitoBean private lateinit var testgrunnlagDAO: TestgrunnlagDAO

  @MockitoBean private lateinit var testResultatDAO: TestResultatDAO

  @Test
  fun `hvis vi har en ferdig test, skal vi kunne lage en retest`() {
    val originalTestgrunnlagId = 1
    val loeysingId = 1
    val kontrollId = 1
    val retestRequest = RetestRequest(originalTestgrunnlagId, loeysingId)

    val originalResultatBrotList =
        listOf(
            ResultatManuellKontroll(
                id = 1,
                testgrunnlagId = originalTestgrunnlagId,
                loeysingId = loeysingId,
                testregelId = 1,
                sideutvalId = 1,
                elementOmtale = "Test omtale",
                elementResultat = TestresultatUtfall.brot,
                elementUtfall = "Feila",
                svar = listOf(ResultatManuellKontrollBase.Svar("1.1", "Ja")),
                kommentar = "Kommentar",
                sistLagra = Instant.now(),
                testVartUtfoert = null))

    `when`(testResultatDAO.getManyResults(originalTestgrunnlagId))
        .thenReturn(Result.success(originalResultatBrotList))

    val originalTestgrunnlag =
        TestgrunnlagKontroll(
            id = 1,
            kontrollId = kontrollId,
            namn = "Original",
            type = TestgrunnlagType.OPPRINNELEG_TEST,
            aktivitet = null,
            datoOppretta = Instant.now())

    `when`(testgrunnlagDAO.getTestgrunnlag(originalTestgrunnlagId))
        .thenReturn(Result.success(originalTestgrunnlag))

    val newTestgrunnlagId = 2
    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId = kontrollId,
            namn = "Retest for kontroll $kontrollId",
            type = TestgrunnlagType.RETEST,
            sideutval = emptyList(),
            testregelIdList = originalResultatBrotList.map { it.testregelId }.distinct())

    `when`(testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag))
        .thenReturn(Result.success(newTestgrunnlagId))

    `when`(testResultatDAO.createRetest(originalResultatBrotList.first()))
        .thenReturn(Result.success(Unit))

    val result = testgrunnlagService.createRetest(retestRequest)

    assertTrue(result.isSuccess)
    assertEquals(newTestgrunnlagId, result.getOrNull())
  }

  @Test
  fun `vi kan ikke lage en retest hvis testen er uten brot`() {
    val originalTestgrunnlagId = 1
    val loeysingId = 1
    val retestRequest = RetestRequest(originalTestgrunnlagId, loeysingId)

    val originalResultatList =
        listOf(
            ResultatManuellKontroll(
                id = 1,
                testgrunnlagId = originalTestgrunnlagId,
                loeysingId = loeysingId,
                testregelId = 1,
                sideutvalId = 1,
                elementOmtale = "Test omtale",
                elementResultat = TestresultatUtfall.samsvar,
                elementUtfall = "Samsvar",
                svar = listOf(ResultatManuellKontrollBase.Svar("1.1", "Ja")),
                kommentar = "Kommentar",
                sistLagra = Instant.now(),
                testVartUtfoert = null))

    `when`(testResultatDAO.getManyResults(originalTestgrunnlagId))
        .thenReturn(Result.success(originalResultatList))

    `when`(testResultatDAO.getManyResults(originalTestgrunnlagId))
        .thenReturn(Result.success(emptyList()))

    val result = testgrunnlagService.createRetest(retestRequest)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    assertEquals(
        "Ingen resultat med brot, kan ikkje køyre retest", result.exceptionOrNull()?.message)
  }
}
