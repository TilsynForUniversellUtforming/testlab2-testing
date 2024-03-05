package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/resultat")
class ResultatResource(
    val aggregeringService: AggregeringService,
    val resultatService: ResultatService
) {

  private val logger = LoggerFactory.getLogger(ResultatResource::class.java)

  @GetMapping
  fun getResultatList(
      @RequestParam testgrunnlagId: Int?,
      @RequestParam maalingId: Int?,
      @RequestParam loeysingId: Int?,
      @RequestParam testregelNoekkel: String?
  ): List<TestresultatDetaljert> {
    logger.debug("Henter resultat for testgrunnlagId: $testgrunnlagId, maalingId: $maalingId")
    if (testgrunnlagId != null) {
      return resultatService.getResulatForManuellKontroll(testgrunnlagId, testregelNoekkel)
    }
    if (maalingId != null) {
      return resultatService.getResultatForAutomatiskMaaling(maalingId, loeysingId)
    }
    return emptyList()
  }

  @PostMapping("/aggregert/{testgrunnlagId}")
  fun createAggregertResultat(@PathVariable testgrunnlagId: Int): ResponseEntity<Any> =
      aggregeringService
          .saveAggregertResultatTestregel(testgrunnlagId)
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
  fun getAggregertResultat(@PathVariable testgrunnlagId: Int): List<AggregertResultatTestregel> =
      aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)
}
