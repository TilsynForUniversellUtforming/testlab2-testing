package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2testing.dto.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TestgrunnlagService(
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val testResultatDAO: TestResultatDAO
) {
  val logger: Logger = LoggerFactory.getLogger(TestgrunnlagService::class.java)

  fun createRetest(retest: RetestRequest): Result<Int> = runCatching {
    val originalResultatBrotList = getOriginalBrotResultat(retest).getOrThrow()
    val retestTestgrunnlagId =
        createRetestTestgrunnlag(retest, originalResultatBrotList).getOrThrow()

    val created = Instant.now()
    originalResultatBrotList
        .map {
          ResultatManuellKontrollBase(
              testgrunnlagId = retestTestgrunnlagId,
              loeysingId = retest.loeysingId,
              testregelId = it.testregelId,
              sideutvalId = it.sideutvalId,
              elementOmtale = it.elementOmtale,
              elementResultat = it.elementResultat,
              elementUtfall = it.elementUtfall,
              svar = it.svar,
              testVartUtfoert = null,
              kommentar = it.kommentar,
              sistLagra = created,
          )
        }
        .forEach { resultat ->
          testResultatDAO.createRetest(resultat).getOrElse {
            logger.error("Kunne ikkje oppdatere resultat for retest", it)
            throw it
          }
        }

    retestTestgrunnlagId
  }

  private fun createRetestTestgrunnlag(
      retest: RetestRequest,
      originalResultatBrotList: List<ResultatManuellKontroll>
  ): Result<Int> = runCatching {
    val (originalTestgrunnlagId, loeysingId) = retest

    val originaltTestgrunnlag =
        testgrunnlagDAO.getTestgrunnlag(originalTestgrunnlagId).getOrElse {
          logger.error("Klarte ikkje å henta testgrunnlag for id $originalTestgrunnlagId: $it")
          throw it
        }

    val kontrollId = originaltTestgrunnlag.kontrollId
    val sideutvalIdsWithBrot = originalResultatBrotList.map { it.sideutvalId }.distinct()

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId = kontrollId,
            namn = "Retest for kontroll $kontrollId",
            type = TestgrunnlagType.RETEST,
            // Vel kun sideutval med brot
            sideutval =
                originaltTestgrunnlag.sideutval.filter {
                  it.loeysingId == loeysingId && sideutvalIdsWithBrot.contains(it.id)
                },
            // Vel kun testreglar med brot
            testregelIdList = originalResultatBrotList.map { it.testregelId }.distinct())

    testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag).getOrElse {
      logger.error(
          "Kunne ikkje opprette testgrunnlag for løysing $loeysingId i kontroll $kontrollId med opprinnelig testgrunnlag $originalTestgrunnlagId",
          it)
      throw it
    }
  }

  private fun getOriginalBrotResultat(
      retest: RetestRequest
  ): Result<List<ResultatManuellKontroll>> = runCatching {
    val (originalTestgrunnlagId, loeysingId) = retest

    val originalResultatList =
        testResultatDAO.getManyResults(originalTestgrunnlagId).getOrElse {
          logger.error(
              "Klarte ikkje å henta testresultat for testgrunnlag $originalTestgrunnlagId: $it")
          throw it
        }

    val brotResultat =
        originalResultatList.filter {
          it.testgrunnlagId == originalTestgrunnlagId &&
              it.loeysingId == loeysingId &&
              it.elementResultat != null &&
              it.elementResultat == TestresultatUtfall.brot
        }

    if (brotResultat.isEmpty()) {
      logger.info("Ingen resultat med brot, kan ikkje køyre retest")
      throw IllegalArgumentException("Ingen resultat med brot, kan ikkje køyre retest")
    }

    brotResultat
  }
}
