package no.uutilsynet.testlab2testing.resultat

import java.net.URI
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.aggregering.AggregertResultatTestregelAPI
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

    // Hentar kun for forenkla kontroll til utfasing av gamalt grensesnitt
    if (maalingId != null) {
      return resultatService.getResultatForAutomatiskMaaling(maalingId, loeysingId)
    }
    return emptyList()
  }

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

  @GetMapping("list")
  fun getListTest(type: Kontrolltype?): ResponseEntity<List<Resultat>> {
    return ResponseEntity.ok(resultatService.getResultatList(type))
  }

  @GetMapping("/kontroll/{id}")
  fun getResultatKontrolll(@PathVariable id: Int): ResponseEntity<List<Resultat>> {
    return ResponseEntity.ok(resultatService.getKontrollResultat(id))
  }

  @GetMapping("/kontroll/{kontrollId}/loeysing/{loeysingId}")
  fun getResultatKontrollLoeysing(
      @PathVariable kontrollId: Int,
      @PathVariable loeysingId: Int
  ): ResponseEntity<List<ResultatOversiktLoeysing>> {
    return ResponseEntity.ok(resultatService.getKontrollLoeysingResultat(kontrollId, loeysingId))
  }

  @GetMapping("/kontroll/{kontrollId}/loeysing/{loeysingId}/krav/{kravId}")
  fun getResultatListKontroll(
      @PathVariable kontrollId: Int,
      @PathVariable loeysingId: Int,
      @PathVariable kravId: Int
  ): List<TestresultatDetaljert> {
    return resultatService.getResultatListKontroll(kontrollId, loeysingId, kravId)
  }

  @GetMapping("/tema")
  fun getResultatPrTema(
      @RequestParam kontrollId: Int?,
      @RequestParam kontrollType: Kontrolltype?,
      @RequestParam fraDato: LocalDate?,
      @RequestParam tilDato: LocalDate?
  ): List<ResultatTema> {
    return resultatService.getResultatPrTema(kontrollId, kontrollType, fraDato, tilDato)
  }

  @GetMapping("/krav")
  fun getResultatPrKrav(
      @RequestParam kontrollId: Int?,
      @RequestParam kontrollType: Kontrolltype?,
      @RequestParam fraDato: LocalDate?,
      @RequestParam tilDato: LocalDate?
  ): List<ResultatKrav> {
    return resultatService.getResultatPrKrav(kontrollId, kontrollType, fraDato, tilDato)
  }
}
