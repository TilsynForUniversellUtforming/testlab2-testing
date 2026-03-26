package no.uutilsynet.testlab2testing.aggregering.repository

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerSideEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AggregeringPerSideRespository : JpaRepository<AggregeringPerSideEntity, Long>
