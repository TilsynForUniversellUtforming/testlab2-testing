package no.uutilsynet.testlab2testing.aggregering.repository

import no.uutilsynet.testlab2testing.aggregering.model.AggregeringPerSuksesskriteriumEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AggregeringPerSuksesskriterium :
    JpaRepository<AggregeringPerSuksesskriteriumEntity, Long>
