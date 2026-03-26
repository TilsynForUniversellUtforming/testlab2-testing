package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.resultat.common.LoysingList
import no.uutilsynet.testlab2testing.resultat.common.ResultatMapper.mapResultatMetaToResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.resultat.external.ResultatMetadataClient
import no.uutilsynet.testlab2testing.resultat.util.TestresultatDetaljertListUtils.paginate
import no.uutilsynet.testlab2testing.resultat.util.TestresultatDetaljertListUtils.sort
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.stereotype.Service

@Service
class ResultatAppResultatService(
    testregelCache: TestregelCache,
    private val externalAggregatedResultsService: ExternalAggregatedResultsService,
    private val resultatMetadataClient: ResultatMetadataClient

) : KontrollResultatService(testregelCache) {
    override fun getKontrollResultat(kontrollId: Int): List<ResultatPerTestregelDTO> {
        println(kontrollId)
        return resultatMetadataClient.getResultatMetadata(kontrollId)
            .flatMap { resultatMetaElement ->
                mapResultatMetaToResultatPerTestregelDTO(
                    resultatMetaElement,
                    externalAggregatedResultsService.getAggregatedDataPerTestregel(resultatMetaElement)
                )
            }
    }

    // ...existing code...
    override fun getBrukararForTest(kontrollId: Int): List<String> {
        return listOf("Testar")
    }

    override fun getResultatForKontroll(
        kontrollId: Int,
        loeysingId: Int
    ): List<TestresultatDetaljert> {
        return getResultatForKontroll(kontrollId)
            .filter { it.loeysingId == loeysingId }
    }

    override fun getResultatForKontroll(kontrollId: Int): List<TestresultatDetaljert> {
        return resultatMetadataClient.getResultatMetadata(kontrollId)
            .flatMap { resultatMetaElement ->
                externalAggregatedResultsService.getResultatDetaljert(resultatMetaElement)
            }
    }

    override fun getResultatForKontroll(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int,
        sortPaginationParams: SortPaginationParams
    ): List<TestresultatDetaljert> {
        return getResultatForKontroll(kontrollId)
            .filter { it.loeysingId == loeysingId && it.testregelId == testregelId }
            .sort(sortPaginationParams)
            .paginate(sortPaginationParams)
    }

    override fun getAlleResultat(): List<ResultatPerTestregelDTO> {
        TODO("Not yet implemented")
    }

    override fun progresjonPrLoeysing(
        resultatData: ResultatPerTestregelDTO,
        loeysingar: LoysingList
    ): Map<Int, Int> {
        return loeysingar.loeysingar.keys.associateWith { 100 }
    }

    override fun getTestresulatDetaljertForKrav(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int,
        sortPaginationParams: SortPaginationParams
    ): List<TestresultatDetaljert> {
        val testreglar = getTestreglarForKrav(kravId).map { it.id }
        return getResultatForKontroll(kontrollId,loeysingId)
        .filter { filterByTestregel(it.testregelId,testreglar) }
            .sort(sortPaginationParams)
            .paginate(sortPaginationParams)
    }

    override fun getTalBrotForKontrollLoeysingTestregel(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int
    ): Result<Int> {
        return getResultatForKontroll(kontrollId).count {
            it.loeysingId == loeysingId
                    && it.testregelId == testregelId
                    && it.elementResultat == TestresultatUtfall.brot
        }
            .let { Result.success(it) }
    }

    override fun getTalBrotForKontrollLoeysingKrav(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int
    ): Result<Int> {
        val testreglar = getTestreglarForKrav(kravId).map { it.id }
        return getResultatForKontroll(kontrollId, loeysingId)
            .count { filterByTestregel(it.testregelId, testreglar) }
            .let { Result.success(it) }
    }
}

