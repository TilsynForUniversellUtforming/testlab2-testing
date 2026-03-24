package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import no.uutilsynet.testlab2testing.resultat.external.api.TestresultatAggregertPerTestregelSearchControllerApi
import no.uutilsynet.testlab2testing.resultat.external.api.TestresultatSearchControllerApi
import no.uutilsynet.testlab2testing.resultat.external.model.EntityModelTestresultat
import no.uutilsynet.testlab2testing.resultat.external.model.EntityModelTestresultatAggregertPerTestregel
import no.uutilsynet.testlab2testing.sideutval.clients.SideutvalClient
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalCache
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExternalAggregatedResultsService(
    private val aggregertPerTestregelClient: TestresultatAggregertPerTestregelSearchControllerApi,
    private val testresultatDetaljertClient: TestresultatSearchControllerApi,
    private val testregelCache: TestregelCache,
    private val sideutvalClient: SideutvalClient,
    private val brukarService: BrukarService
) : AggregatedResultsInterface {

    override fun getAggregatedDataPerTestregel(resultatMeta: ResultatMetadata): List<AggregeringPerTestregelEntity> {

        return getResultElements(resultatMeta)
            .map { aggregert ->
                AggregeringPerTestregelEntity(
                    testrunUuid = UUID.fromString(resultatMeta.testrunUuid),
                    testregelId = aggregert.testregelId
                        ?: throw IllegalArgumentException("testregelId kan ikkje være null"),
                    loeysingId = aggregert.loeysingId
                        ?: throw IllegalArgumentException("loeysingId kan ikkje være null"),
                    suksesskriterium = testregelCache.getTestregelById(aggregert.testregelId).krav.id,
                    fleire_suksesskriterium = emptyList(),
                    talElementSamsvar = aggregert.talElementSamsvar ?: 0,
                    talElementBrot = aggregert.talElementBrot ?: 0,
                    talElementVarsel = aggregert.talElementVarsel ?: 0,
                    talSiderSamsvar = aggregert.talSiderSamsvar ?: 0,
                    talSiderBrot = aggregert.talSiderBrot ?: 0,
                    talSiderIkkjeForekomst = aggregert.talSiderIkkjeForekomst ?: 0,
                    testregelGjennomsnittlegSideBrotProsent = aggregert.testregelGjennomsnittlegSideBrotProsent,
                    testregelGjennomsnittlegSideSamsvarProsent = aggregert.testregelGjennomsnittlegSideSamsvarProsent,
                    id = null,
                    maalingId = null,
                    testgrunnlagId = null
                )
            }
    }

    private fun getResultElements(resultatMeta: ResultatMetadata): List<EntityModelTestresultatAggregertPerTestregel> {
        return aggregertPerTestregelClient.findByTestrunUuid(resultatMeta.testrunUuid)
            .body?.embedded?.testresultatAggregertPerTestregel
            ?: throw IllegalArgumentException("Tomt resultat")
    }

    private fun getResultElementsDetaljert(resultatMeta: ResultatMetadata) =
        testresultatDetaljertClient.findByTestrunUuid(resultatMeta.testrunUuid)
            .body?.embedded?.testresultat
            ?: throw IllegalArgumentException("Tomt resultat")

    fun getResultatDetaljert(resultatMeta: ResultatMetadata) : List<TestresultatDetaljert> {
        val result = getResultElementsDetaljert(resultatMeta)

        val sideutvalMap = sideutvalCacheGetSideutvalUrl(result.map { it.loeysingId })

        return result.map { element ->

            val testregel = getTestregelFromId(element.testregelId)
            TestresultatDetaljert(
                resultatId = null,
                loeysingId = element.loeysingId!!,
                testregelId = testregel.id,
                testregelNoekkel = testregel.testregelId,
                testgrunnlagId = resultatMeta.testgrunnlagId!!,
                side = sideutvalMap[element.sideutvalId!!].orEmpty(),
                suksesskriterium = listOf(testregel.krav.suksesskriterium),
                testVartUtfoert = element.testUtfoert?.toLocalDateTime(),
                elementUtfall = element.elementUtfall,
                elementResultat = TestresultatUtfall.valueOf(element.elementResultat!!.value),
                elementOmtale = elementOmtale(element),
                brukarId = brukarService.getBrukarById(element.brukarId!!),
                kommentar = null,
                bilder = null
            )
        }
    }

    private fun elementOmtale(element: EntityModelTestresultat): TestresultatDetaljert.ElementOmtale =
        TestresultatDetaljert.ElementOmtale(
            element.elementOmtaleHtml,
            element.elementOmtalePointer,
            element.elementOmtaleDescription
        )

    private fun getTestregelFromId(testregelId: Int?): TestregelAggregate {
        requireNotNull(testregelId)
        return testregelCache.getTestregelById(testregelId)
    }

    private fun sideutvalCacheGetSideutvalUrl(loeysingList:List<Int?>): Map<Int,String> {
        val resultMap = mutableMapOf<Int,String>()
        loeysingList.map { loeysingId ->
            requireNotNull(loeysingId)
            sideutvalClient.lookupLoeysing(loeysingId)
        }.forEach { resultMap.putAll( it ) }
        return resultMap
    }

}