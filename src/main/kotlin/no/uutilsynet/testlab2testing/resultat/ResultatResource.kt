package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/resultat")
class ResultatResource(
    val aggregeringService: AggregeringService,
    val resultatService: ResultatService
) {

  @GetMapping
  fun getResultatList(
      @RequestParam sakId: Int?,
      @RequestParam maalingId: Int?,
      @RequestParam loeysingId: Int?
  ): List<TestresultatDetaljert> {
    if (sakId != null) {
      return resultatService.getResulatForManuellKontroll(sakId)
    }
    if (maalingId != null) {
      return resultatService.getResultatForAutomatiskMaaling(maalingId, loeysingId)
    }
    return emptyList()
  }

  @PostMapping("/aggregert/{testgrunnlagId}")
  fun createAggregertResultat(@PathVariable testgrunnlagId: Int) =
      aggregeringService.saveAggregertResultatTestregel(testgrunnlagId)

  @GetMapping("/aggregert/{testgrunnlagId}")
  fun getAggregertResultat(@PathVariable testgrunnlagId: Int) =
      aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)
}
