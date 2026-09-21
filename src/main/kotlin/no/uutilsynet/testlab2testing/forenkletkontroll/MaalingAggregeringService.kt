package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDTO
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class MaalingAggregeringService(
    private val aggregeringService: AggregeringService,
    private val maalingService: MaalingService,
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  fun hentEllerGenererAggregeringPrSide(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "side")) {
      maalingService.getFerdigeTestkoeyringar(maalingId).forEach {
        aggregeringService.saveAggregeringSideAutomatisk(it.lenker, it.loeysingId)
      }
    }
    return aggregeringService.getAggregertResultatSide(maalingId).let { ResponseEntity.ok(it) }
  }

  fun hentEllerGenererAggregeringPrSuksesskriterium(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "suksesskriterium")) {
      maalingService.getFerdigeTestkoeyringar(maalingId).forEach {
        aggregeringService.saveAggregertResultatSuksesskriteriumAutomatisk(it.lenker, it.loeysingId)
      }
    }
    return aggregeringService.getAggregertResultatSuksesskriterium(maalingId).let {
      ResponseEntity.ok(it)
    }
  }

  fun hentEllerGenererAggregeringPrTestregel(maalingId: Int): ResponseEntity<Any> {
    if (!aggregeringService.harMaalingLagraAggregering(maalingId, "testresultat")) {
      logger.info("Aggregering er ikkje generert for måling $maalingId, genererer no")
      maalingService.getFerdigeTestkoeyringar(maalingId).forEach {
        aggregeringService.saveAggregertResultatTestregelAutomatisk(it.lenker, it.loeysingId)
      }
    }
    return aggregeringService.getAggregertResultatTestregel(maalingId).let { ResponseEntity.ok(it) }
  }

  fun reimportAggregeringar(maalingId: Int, loeysingId: Int?) {
    runCatching {
      val maalingFerdig = maalingService.isMaalingFerdigTestet(maalingId)
      require(maalingFerdig) { "Måling er ikkje ferdig testa" }

      maalingService
          .getFerdigeTestkoeyringar(maalingId)
          .filter { filterTestkoeyring(it, loeysingId) }
          .forEach { aggregeringService.saveAggregering(it.lenker, it.loeysingId) }
    }
  }

  private fun filterTestkoeyring(testKoeyring: TestkoeyringDTO, loeysingId: Int?): Boolean {
    if (loeysingId != null) {
      return testKoeyring.loeysingId == loeysingId
    }
    return true
  }
}
