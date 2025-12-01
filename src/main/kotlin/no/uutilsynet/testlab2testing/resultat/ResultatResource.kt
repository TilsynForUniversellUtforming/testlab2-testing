package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.net.URI
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.common.SortOrder
import no.uutilsynet.testlab2testing.common.SortPaginationParams
import no.uutilsynet.testlab2testing.common.SortParamTestregel
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregelAPI
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
      @RequestParam loeysingId: Int?
  ): List<TestresultatDetaljert> {
    logger.debug("Henter resultat for testgrunnlagId: $testgrunnlagId, maalingId: $maalingId")

    // Hentar kun for forenkla kontroll til utfasing av gamalt grensesnitt
    if (maalingId != null) {
      return resultatService.getResultatForMaaling(maalingId, loeysingId)
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

  @Observed(name = "ResultatResource.getResultatListKontroll")
  @GetMapping("/kontroll/{kontrollId}/loeysing/{loeysingId}/krav/{testregelId}")
  fun getResultatListKontroll(
      @PathVariable kontrollId: Int,
      @PathVariable loeysingId: Int,
      @PathVariable testregelId: Int,
      @RequestParam size: Int = 20,
      @RequestParam page: Int = 0,
      @RequestParam sortParam: SortParamTestregel?,
      @RequestParam sortOrder: SortOrder?,
  ): List<TestresultatDetaljert> {

    val sortPaginationParams =
        SortPaginationParams(
            sortParam = sortParam ?: SortParamTestregel.side,
            sortOrder = sortOrder ?: SortOrder.ASC,
            pageNumber = page,
            pageSize = size)
    return resultatService.getTestresultatDetaljerPrTestregel(
        kontrollId, loeysingId, testregelId, sortPaginationParams)
  }

  @GetMapping("/tema")
  fun getResultatPrTema(
      @RequestParam kontrollId: Int?,
      @RequestParam kontrollType: Kontrolltype?,
      @RequestParam loeysingId: Int?,
      @RequestParam fraDato: LocalDate?,
      @RequestParam tilDato: LocalDate?
  ): List<ResultatTema> {
    return resultatService.getResultatPrTema(kontrollId, kontrollType, loeysingId, fraDato, tilDato)
  }

  @GetMapping("/krav")
  fun getResultatPrKrav(
      @RequestParam kontrollId: Int?,
      @RequestParam kontrollType: Kontrolltype?,
      @RequestParam loeysingId: Int?,
      @RequestParam fraDato: LocalDate?,
      @RequestParam tilDato: LocalDate?
  ): List<ResultatKrav> {
    return resultatService.getTestresultatDetaljertPrKrav(
        kontrollId, kontrollType, loeysingId, fraDato, tilDato)
  }
}
