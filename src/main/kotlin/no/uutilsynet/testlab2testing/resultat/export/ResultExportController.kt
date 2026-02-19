package no.uutilsynet.testlab2testing.resultat.export

import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringDAO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelExport
import no.uutilsynet.testlab2testing.testresultat.model.TestresultatExport
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/resultat/export")
class ResultExportController(
    private val testresultatDAO: TestresultatDAO,
    private val resultatClient: ResultatClient,
    private val aggregeringDAO: AggregeringDAO,
    private val resultatExportMapper: ResultatExportMapper,
) {

  val logger = LoggerFactory.getLogger(ResultExportController::class.java)

  @GetMapping("testgrunnlag/{testgrunnlagId}")
  fun getTestresultForExport(
      @PathVariable("testgrunnlagId") testgrunnlagId: Int
  ): ResponseEntity<List<TestresultatExport>> {
    return ResponseEntity.ok().body(testresultatDAO.getTestresultatByTestgrunnlagId(testgrunnlagId))
  }

  @GetMapping("maaling/{maalingId}/loeysing/{loeysingId}")
  fun getTestresultForExportForMaaling(
      @PathVariable("maalingId") maalingId: Int,
      @PathVariable("loeysingId") loeysingId: Int
  ): ResponseEntity<List<TestresultatExport>> {
    return ResponseEntity.ok()
        .body(testresultatDAO.getTestresultatByMaalingId(maalingId, loeysingId))
  }

  @PostMapping("maaling/{maalingId}/loeysing/{loeysingId}")
  fun exportTestresultForMaaling(
      @PathVariable("maalingId") maalingId: Int,
      @PathVariable("loeysingId") loeysingId: Int,
  ): ResponseEntity<List<Long>> {
    val testresultatList = testresultatDAO.getTestresultatByMaalingId(maalingId, loeysingId)
    return resultatClient
        .putTestresultatList(testresultatList)
        .fold(
            { idList -> ResponseEntity.ok().body(idList) },
            { _ -> ResponseEntity.internalServerError().build() })
  }

  @PostMapping("testgrunnlag/{testgrunnlagId}")
  fun exportTestresultForTestgrunnlag(
      @PathVariable("testgrunnlagId") testgrunnlagId: Int,
  ): ResponseEntity<List<Long>> {
    val testresultatList = testresultatDAO.getTestresultatByTestgrunnlagId(testgrunnlagId)

    return resultatClient
        .putTestresultatList(testresultatList)
        .fold(
            { idList -> ResponseEntity.ok().body(idList) },
            { error ->
              logger.error(error.message)
              ResponseEntity.internalServerError().build()
            })
  }

    @GetMapping("aggregering/testregel/{maalingId}")
    fun getAggregeringForMaaling(
        @PathVariable("maalingId") maalingId: Int
    ): ResponseEntity<List<AggregeringPerTestregelExport>> {
        val aggregering = resultatExportMapper.getAggregeringForMaaling(maalingId)
        return ResponseEntity.ok().body(aggregering)
    }

    @PostMapping("aggregering/testregel/{maalingId}")
    fun exportAggregeringForMaaling(
        @PathVariable("maalingId") maalingId: Int
    ): ResponseEntity<List<Long>> {
        val aggregering = resultatExportMapper.getAggregeringForMaaling(maalingId)
        return resultatClient.putAggregeringPerTestregelList(aggregering)
            .fold(
                { idList -> ResponseEntity.ok().body(idList) },
                { error ->
                    logger.error(error.message)
                    ResponseEntity.internalServerError().build()
                })
    }
}
