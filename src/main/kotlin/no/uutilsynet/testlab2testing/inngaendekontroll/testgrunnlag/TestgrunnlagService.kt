package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import java.time.Instant
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatDAO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TestgrunnlagService(
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO,
    @Autowired val testResultatDAO: TestResultatDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
) {
  val logger: Logger = LoggerFactory.getLogger(TestgrunnlagService::class.java)

  fun createOrUpdateFromKontroll(testgrunnlag: NyttTestgrunnlagFromKontroll): Result<Int> {
    val opprinneligTestgrunnlag =
        testgrunnlagDAO.getOpprinneligTestgrunnlag(testgrunnlag.kontrollId)

    return if (opprinneligTestgrunnlag.isSuccess) {
      opprinneligTestgrunnlag
          .mapCatching { testgrunnlagDAO.getTestgrunnlag(it) }
          .mapCatching { updateExisting(it.getOrThrow(), testgrunnlag) }
          .map { it.getOrThrow().id }
    } else {
      testgrunnlagDAO.createTestgrunnlag(testgrunnlag.toNyttTestgrunnlag())
    }
  }

  fun kontrollHasTestresultat(kontrollId: Int): Boolean =
      testgrunnlagDAO.kontrollHasTestresultat(kontrollId)

  fun createRetest(retest: RetestRequest): Result<Int> = runCatching {
    logger.debug(
        "Retest for originalt testgrunnlag ${retest.originalTestgrunnlagId} og løysing ${retest.loeysingId}")

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

  private fun updateExisting(
      eksisterendeTestgrunnlag: TestgrunnlagKontroll,
      testgrunnlag: NyttTestgrunnlagFromKontroll
  ) =
      testgrunnlagDAO.updateTestgrunnlag(
          eksisterendeTestgrunnlag.copy(
              namn = testgrunnlag.namn,
              testreglar = testgrunnlag.testregelIdList,
              sideutval = testgrunnlag.sideutval,
          ))

  fun NyttTestgrunnlagFromKontroll.toNyttTestgrunnlag(): NyttTestgrunnlag {
    return NyttTestgrunnlag(
        kontrollId = this.kontrollId,
        namn = this.namn,
        type = this.type,
        sideutval = this.sideutval,
        testregelIdList = this.testregelIdList.map { it.id })
  }

  fun getLoeysingForTestgrunnlag(testgrunnlagId: Int): List<Loeysing> {
    val loeysingIdList =
        testgrunnlagDAO.getSideutvalForTestgrunnlag(testgrunnlagId).map { it.loeysingId }
    val loeysingList =
        loeysingsRegisterClient
            .getMany(loeysingIdList, Instant.now())
            .fold(
                onSuccess = { it },
                onFailure = {
                  logger.error(
                      "Feil ved henting av løysingar $loeysingIdList for testgrunnlag $testgrunnlagId",
                      it)
                  throw it
                })
    return loeysingList
  }

  fun hasTestgrunnlagTestregel(testregelId: Int): Boolean {
    return testgrunnlagDAO.hasTestgrunnlagTestregel(testregelId)
  }
}
