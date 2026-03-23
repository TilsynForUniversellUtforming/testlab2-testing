package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import no.uutilsynet.testlab2testing.resultat.external.api.TestresultatAggregertPerTestregelSearchControllerApi
import no.uutilsynet.testlab2testing.resultat.external.model.EntityModelTestresultatAggregertPerTestregel
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import org.springframework.stereotype.Service
import java.util.*

@Service
class ExternalAggregatedResultsService(
    private val aggregertPerTestregelClient: TestresultatAggregertPerTestregelSearchControllerApi,
    private val testregelCache: TestregelCache,
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

}