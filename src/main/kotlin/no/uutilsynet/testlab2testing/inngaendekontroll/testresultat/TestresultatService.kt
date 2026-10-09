package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestResultatResource.CreateTestResultat
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutomaticTestingService
import no.uutilsynet.testlab2testing.testing.automatisk.elementtesting.AutotesterPayload
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import org.springframework.stereotype.Service

@Service
class TestresultatService(val testregelCache: TestregelCache, val automaticTestingService: AutomaticTestingService) {

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
                        elementResultat = TestresultatUtfall.valueOf(it.elementResultat),
                        elementUtfall = it.elementUtfall)
                },
                onFailure = { createTestResult })
    }

//    fun checkIfUtfallIsCustom(testResultat: ResultatManuellKontroll): Boolean {
//        val testregel = testregelCache.getTestregelById(testResultat.testregelId)
//        val canHaveCustomOutcome = testregel.modus == TestregelModus.manuellForenkla
//    }

}