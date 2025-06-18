package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO

sealed class KontrollResultatService(
    val resultatDAO: ResultatDAO,
    val testregelDAO: TestregelDAO,
    val kravregisterClient: KravregisterClient
) {

  protected fun filterByTestregel(testregelId: Int, testregelIdsForKrav: List<Int>): Boolean {
    return testregelIdsForKrav.contains(testregelId)
  }

  protected fun getTestregelIdFromSchema(testregelKey: String): Testregel {
    return testregelDAO.getTestregelByTestregelId(testregelKey)
        ?: throw RuntimeException("Fant ikkje testregel for testregelId $testregelKey")
  }

  protected fun getTesteregelFromId(testregelId: Int): Testregel {
    return testregelDAO.getTestregel(testregelId)
        ?: throw RuntimeException("Fant ikkje testregel for id $testregelId")
  }

  protected fun getSuksesskriteriumFromTestregel(kravId: Int): List<String> {
    return listOf(kravregisterClient.getSuksesskriteriumFromKrav(kravId))
  }

  open fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    throw NotImplementedError("Ikke implementert")
  }

  abstract fun getBrukararForTest(kontrollId: Int): List<String>
}
