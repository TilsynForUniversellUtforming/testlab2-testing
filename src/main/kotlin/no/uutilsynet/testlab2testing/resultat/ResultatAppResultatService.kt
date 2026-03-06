package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert

class ResultatAppResultatService(
    resultatDAO: ResultatDAO,
    kravregisterClient: KravregisterClient,
    testresultatDAO: TestresultatDAO,
    testregelCache: TestregelCache
) : KontrollResultatService(resultatDAO, kravregisterClient, testresultatDAO, testregelCache) {
  override fun getBrukararForTest(kontrollId: Int): List<String> {
    TODO("Not yet implemented")
  }

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int
  ): List<TestresultatDetaljert> {
    TODO("Not yet implemented")
  }

  override fun getResultatForKontroll(kontrollId: Int): List<TestresultatDetaljert> {
    TODO("Not yet implemented")
  }

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDetaljert> {
    TODO("Not yet implemented")
  }

  override fun getAlleResultat(): List<ResultatLoeysingDTO> {
    TODO("Not yet implemented")
  }

  override fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      loeysingar: ResultatService.LoysingList
  ): Map<Int, Int> {
    TODO("Not yet implemented")
  }

  override fun getTestresulatDetaljertForKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int,
      sortPaginationParams: SortPaginationParams
  ): List<TestresultatDetaljert> {
    TODO("Not yet implemented")
  }

  override fun getTalBrotForKontrollLoeysingTestregel(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int
  ): Result<Int> {
    TODO("Not yet implemented")
  }

  override fun getTalBrotForKontrollLoeysingKrav(
      kontrollId: Int,
      loeysingId: Int,
      kravId: Int
  ): Result<Int> {
    TODO("Not yet implemented")
  }
}
