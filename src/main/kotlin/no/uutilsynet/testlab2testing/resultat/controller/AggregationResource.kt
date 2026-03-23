package no.uutilsynet.testlab2testing.resultat.controller

import no.uutilsynet.testlab2testing.resultat.ResultatResource
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregelAPI
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/resultat")
class AggregationResource( val aggregeringService: AggregeringService,) {


    private val logger = LoggerFactory.getLogger(AggregationResource::class.java)

    @PostMapping("/aggregert/{testgrunnlagId}")
    fun createAggregertResultat(@PathVariable testgrunnlagId: Int): ResponseEntity<Any> =
        aggregeringService
            .saveAggregertResultat(testgrunnlagId)
            .fold(
                onSuccess = {
                    return ResponseEntity.created(URI.create("/resultat/aggregert/${testgrunnlagId}"))
                        .build()
                },
                onFailure = {
                    logger.error("Feil ved oppretting av aggregert resultat", it)
                    ResponseEntity.internalServerError().build()
                })

    @GetMapping("/aggregert/{testgrunnlagId}")
    fun getAggregertResultat(@PathVariable testgrunnlagId: Int): List<AggregertResultatTestregelAPI> =
        aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)
}