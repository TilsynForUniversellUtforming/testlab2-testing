package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.common.Constants
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutomaticTestingService
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutotesterPayload
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestresultatService(
    val testregelCache: TestregelCache,
    val automaticTestingService: AutomaticTestingService,
    val testResultatDAO: TestResultatDAO,
    val brukarService: BrukarService,
    val dataUrlConverter: DataUrlConverter,
    val bildeService: BildeService,
    ) {

    val logger = LoggerFactory.getLogger(this::class.java)

  fun getResultAutomatic(createTestResult: CreateTestResultat): CreateTestResultat {
    val testregel = testregelCache.getTestregelById(createTestResult.testregelId)
    return automaticTestingService
        .testElement(
            AutotesterPayload(
                htmlElement = createTestResult.elementOmtaleHtml ?: "",
                qualwebRule = testregel.testregelId))
        .fold(
            onSuccess = {
              createTestResult.copy(
                  element =
                      createTestResult.element?.copy(
                          elementResultat = TestresultatUtfall.valueOf(it.elementResultat),
                          elementUtfall = it.elementUtfall,
                      )
                          ?: Element(
                              elementOmtale = createTestResult.elementOmtale,
                              elementResultat = TestresultatUtfall.valueOf(it.elementResultat),
                              elementUtfall = it.elementUtfall,
                          ))
            },
            onFailure = { createTestResult })
  }

    fun deleteOldResultsAndUpdate(newResult: CreateTestResultat): Result<Int> {
        return runCatching {
            val existingResults = testResultatDAO.findByTestgrunnlagAndLoeysingAndTestregelAndSide(
                newResult.testgrunnlagId,
                newResult.loeysingId,
                newResult.testregelId,
                newResult.sideutvalId
            ).getOrThrow()

            existingResults.forEach { testResult ->
                testResultatDAO.delete(testResult)
            }
            existingResults.size
        }.onFailure { throw it }
    }

    @Transactional
    fun createTestresultat(createTestResultat: CreateTestResultat): Result<Int> {
        return runCatching {
            val brukar = brukarService.getCurrentUser()

            val resultToSave = resolveResultToSave(createTestResultat)

            val testresultatId =
                testResultatDAO.save(resultToSave.copy(brukar = brukar)).getOrThrow()

            if (!createTestResultat.imageDataUrl.isNullOrBlank()) {
                saveImage(testresultatId, createTestResultat.imageDataUrl)
            }
            testresultatId
        }
    }

    private fun resolveResultToSave(input: CreateTestResultat): CreateTestResultat {
        if (isTestregelAutomatic(input.testregelId)) {
            return getResultAutomatic(input).takeIf { it.elementResultat != null }
                ?: input
        }
        if (input.element?.elementOmtale == Constants.ELEMENT_OMTALE_SIDE) {
            deleteOldResultsAndUpdate(input).onFailure { throw it }
        }
        return input
    }


    private fun saveImage(testresultatId: Int, bildeDataUrl: String) {
        val bilde = dataUrlConverter.dataUrlToImage(bildeDataUrl)
        bildeService.createBilde(testresultatId, listOf(bilde)).onFailure {
            logger.error("Feil ved opplasting av bilder", it)
            throw it
        }
    }

    private fun isTestregelAutomatic(testregelId: Int): Boolean {
        val testregel = testregelCache.getTestregelById(testregelId)
        return testregel.modus == TestregelModus.automatisk
    }

}
