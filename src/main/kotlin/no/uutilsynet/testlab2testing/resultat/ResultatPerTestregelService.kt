package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.aggregering.repository.AggregeringPerTestregelRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ResultatPerTestregelService(private val aggregeringPerTestregelRepository: AggregeringPerTestregelRepository) {
}

/*
data class ResultatPerTestregelDTO(
    val id: Int,
    val testgrunnlagId: Int,
    val namn: String,
    val testType: TestgrunnlagType,
    val dato: LocalDate,
    val loeysingId: Int,
    val score: Double,
    val talElementSamsvar: Int,
    val talElementBrot: Int,
    val testregelId: Int,
)*/
