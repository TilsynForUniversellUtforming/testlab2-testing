package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import java.util.UUID

interface AggregatedResultsInterface {

    fun getAggregatedDataPerTestregel(resultatMeta: ResultatMetadata): List<AggregeringPerTestregelEntity>
}