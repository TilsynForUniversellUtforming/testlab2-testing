package no.uutilsynet.testlab2testing.aggregering.repository

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AggregeringPerTestregelRepository : JpaRepository<AggregeringPerTestregelEntity, Long> {
    fun findByTestrunUuid(testrunUuid: UUID): kotlin.collections.MutableList<no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity>
}
