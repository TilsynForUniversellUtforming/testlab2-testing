package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/resultat/import")
class ResultatImportController(
    private val azureStorage2DbService: AzureStorage2DbService,
    private val maalingDAO: MaalingDAO
) {

  @GetMapping("/maaling/{maalingId}/loeysing/{loeysingId}")
  fun importerResultat(@PathVariable maalingId: Int, @PathVariable loeysingId: Int): Int {
    val results =
        azureStorage2DbService.createTestresultatDB(maalingId = maalingId, loeysingId = loeysingId)
    return results.getOrThrow().size
  }

  @GetMapping("/maaling/{maalingId}")
  fun importMaaling(@PathVariable maalingId: Int) {
    val loeysinger = maalingDAO.getLoeysingarForMaaling(maalingId)
    loeysinger.forEach { loeysing ->
      azureStorage2DbService.createTestresultatDB(maalingId = maalingId, loeysingId = loeysing.id)
    }
  }
}
