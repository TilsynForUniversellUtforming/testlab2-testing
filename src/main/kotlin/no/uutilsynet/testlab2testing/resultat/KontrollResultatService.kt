package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatService.LoysingList
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

  protected fun List<TestresultatDetaljert>.filterBrot(): List<TestresultatDetaljert> {
    return this.filter { it.elementResultat == TestresultatUtfall.brot }
  }

  open fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    throw NotImplementedError("Ikke implementert")
  }

  abstract fun getBrukararForTest(kontrollId: Int): List<String>

  abstract fun getResultatForKontroll(kontrollId: Int, loeysingId: Int): List<TestresultatDetaljert>

  abstract fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int,
  ): List<TestresultatDetaljert>

  abstract fun getAlleResultat(): List<ResultatLoeysingDTO>

  abstract fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      loeysingar: LoysingList,
  ): Map<Int, Int>

  fun getResultatBrotForKontroll(kontrollId: Int, loeysingId: Int): List<TestresultatDetaljert> {
    return getResultatForKontroll(kontrollId, loeysingId).filterBrot()
  }
}
