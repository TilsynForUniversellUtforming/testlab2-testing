package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URL
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser.ExtractedElement
import no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser.ImportService
import no.uutilsynet.testlab2testing.inngaendekontroll.preprosesser.TestresultatBase
import org.springframework.stereotype.Service

@Service
class TestresultatService(val importService: ImportService, val sideutvalDAO: SideutvalDAO) {

  fun importTestelement(
      testresultatBase: TestresultatBase,
      xpathString: String
  ): List<CreateTestResultat> {

    return runCatching {
          val url = getUrlFromSideutvalId(testresultatBase)

          extractedTestElements(xpathString, url).map { element ->
            createTestResultat(testresultatBase, element)
          }
        }
        .fold(
            onSuccess = { it },
            onFailure = { throw RuntimeException("Error importing testelement", it) })
  }

  private fun createTestResultat(
      testresultatBase: TestresultatBase,
      element: ExtractedElement
  ): CreateTestResultat = importService.createTestElement(testresultatBase, element)

  private fun extractedTestElements(xpathString: String, url: URL): List<ExtractedElement> =
      importService.getElements(xpathString, url)
          ?: throw NoSuchElementException("No elements found")

  private fun getUrlFromSideutvalId(testresultatBase: TestresultatBase): URL {
    val url =
        sideutvalDAO
            .getSideutvalUrlMapKontroll(listOf(testresultatBase.sideutvalId))[
                testresultatBase.sideutvalId]
    requireNotNull(url) { "URL for sideutvalId ${testresultatBase.sideutvalId} is null" }

    return url
  }
}
