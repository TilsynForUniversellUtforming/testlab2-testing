package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.resultat.common.LoysingList
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.stereotype.Service

@Service
class ResultatAppResultatService(
    testregelCache: TestregelCache
) : KontrollResultatService(testregelCache) {
    override fun getKontrollResultat(kontrollId: Int): List<ResultatPerTestregelDTO> {
        TODO("Not yet implemented")
    }

    // ...existing code...
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

    override fun getAlleResultat(): List<ResultatPerTestregelDTO> {
        TODO("Not yet implemented")
    }

    override fun progresjonPrLoeysing(
        testgrunnlagId: Int,
        loeysingar: LoysingList
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

