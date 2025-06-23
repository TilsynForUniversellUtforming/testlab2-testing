package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelService

sealed class KontrollResultatService(
    val resultatDAO: ResultatDAO,
    val kravregisterClient: KravregisterClient,
    val testregelService: TestregelService
) {

  protected fun filterByTestregel(testregelId: Int, testregelIdsForKrav: List<Int>): Boolean {
    return testregelIdsForKrav.contains(testregelId)
  }

  protected fun getTestregelIdFromSchema(testregelKey: String): Testregel {
    return testregelService.getTestregelByTestregelId(testregelKey)
  }

  protected fun getTesteregelFromId(testregelId: Int): Testregel {
    return testregelService.getTestregel(testregelId)
  }

  protected fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return listOf(kravregisterClient.getSuksesskriteriumFromKrav(kravId))
  }

  open fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    throw NotImplementedError("Ikke implementert")
  }

  abstract fun getBrukararForTest(kontrollId: Int): List<String>
}
