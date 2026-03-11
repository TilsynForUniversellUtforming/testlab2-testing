package no.uutilsynet.testlab2testing.aggregering.repository

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerTestregelEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AggregeringPerTestregelRepository : JpaRepository<AggregeringPerTestregelEntity, Long>
