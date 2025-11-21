package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.resultat.ResultatService.LoysingList
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav

sealed class KontrollResultatService(
    protected val resultatDAO: ResultatDAO,
    protected val kravregisterClient: KravregisterClient,
    protected val testregelCache: TestregelCache
) {

  protected fun filterByTestregel(testregelId: Int, testregelIdsForKrav: List<Int>): Boolean {
    return testregelIdsForKrav.contains(testregelId)
  }

  protected fun getTesteregelFromId(testregelId: Int): TestregelKrav {
    return testregelCache.getTestregelById(testregelId)
  }

  private fun List<TestresultatDetaljert>.filterBrot(): List<TestresultatDetaljert> {
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
