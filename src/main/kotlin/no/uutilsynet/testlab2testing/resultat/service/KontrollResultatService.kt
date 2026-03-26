package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.resultat.common.LoysingList
import no.uutilsynet.testlab2testing.resultat.repository.ResultatDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert

sealed class KontrollResultatService(
    protected val testregelCache: TestregelCache,
) {

  protected fun filterByTestregel(testregelId: Int, testregelIdsForKrav: List<Int>): Boolean {
    return testregelIdsForKrav.contains(testregelId)
  }

  protected fun getTesteregelFromId(testregelId: Int): TestregelAggregate {
    return testregelCache.getTestregelById(testregelId)
  }

  private fun List<TestresultatDetaljert>.filterBrot(): List<TestresultatDetaljert> {
    return this.filter { it.elementResultat == TestresultatUtfall.brot }
  }

  abstract fun getKontrollResultat(kontrollId: Int): List<ResultatPerTestregelDTO>

  abstract fun getBrukararForTest(kontrollId: Int): List<String>

  abstract fun getResultatForKontroll(kontrollId: Int, loeysingId: Int): List<TestresultatDetaljert>

  abstract fun getResultatForKontroll(kontrollId: Int): List<TestresultatDetaljert>

  abstract fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      sortPaginationParams: SortPaginationParams,
  ): List<TestresultatDetaljert>

  abstract fun getAlleResultat(): List<ResultatPerTestregelDTO>

  abstract fun progresjonPrLoeysing(
      resultatData: ResultatPerTestregelDTO,
      loeysingar: LoysingList,
  ): Map<Int, Int>

  fun getResultatBrotForKontroll(kontrollId: Int, loeysingId: Int): List<TestresultatDetaljert> {
    return getResultatForKontroll(kontrollId, loeysingId).filterBrot()
  }

  abstract fun getTestresulatDetaljertForKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDetaljert>

  protected fun getTestreglarForKrav(kravId: Int): List<TestregelAggregate> {
    return testregelCache.getTestregelByKravId(kravId)
  }

  abstract fun getTalBrotForKontrollLoeysingTestregel(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
  ): Result<Int>

  abstract fun getTalBrotForKontrollLoeysingKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): Result<Int>
}
