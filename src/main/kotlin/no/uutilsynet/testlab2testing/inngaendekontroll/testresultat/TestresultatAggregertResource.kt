package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/testresultat")
class TestresultatAggregertResource(
    val aggregeringService: AggregeringService,
) {

    @PostMapping("/aggregert/{testgrunnlagId}")
    fun createAggregertResultat(@PathVariable testgrunnlagId: Int) =
        aggregeringService.saveAggregertResultat(testgrunnlagId)

    @GetMapping("/aggregert/{testgrunnlagId}")
    fun getAggregertResultat(@PathVariable testgrunnlagId: Int) =
        aggregeringService.getAggregertResultatTestregelForTestgrunnlag(testgrunnlagId)
}