package no.uutilsynet.testlab2testing.resultat.service

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import no.uutilsynet.testlab2testing.aggregering.repository.AggregeringPerTestregelRepository
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DBAggregatedResults(
    private val aggregeringPerTestregelRepository: AggregeringPerTestregelRepository): AggregatedResultsInterface {
    override fun getAggregatedDataPerTestregel(resultatMeta: ResultatMetadata): List<AggregeringPerTestregelEntity> {
        return aggregeringPerTestregelRepository
            .findByTestrunUuid(UUID.fromString(resultatMeta.testrunUuid))
            .toList()
    }


}