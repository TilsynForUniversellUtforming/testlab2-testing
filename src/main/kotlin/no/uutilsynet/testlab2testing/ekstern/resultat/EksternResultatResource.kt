package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(@Autowired val eksternResultatService: EksternResultatService) {

  private val logger = LoggerFactory.getLogger(EksternResultatResource::class.java)

  @GetMapping
  fun findTestForOrgNr(
      @RequestParam("orgnr") orgnr: String
  ): ResponseEntity<TestListElementEkstern?> {
    logger.debug("Henter tester for orgnr $orgnr")

    return try {
      val result = eksternResultatService.findTestForOrgNr(orgnr).getOrThrow()
      ResponseEntity.ok(result)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build<TestListElementEkstern>()
    } catch (e: Exception) {
      logger.error(e.message)
      ResponseEntity.badRequest().build<TestListElementEkstern>()
    }
  }

  //  @GetMapping("rapport/{rapportId}")
  //  fun getResultatRapport(@PathVariable rapportId: String): ResponseEntity<out Any> {
  //    return kotlin
  //        .runCatching {
  //          val kontrollLoeysingList: List<KontrollIdLoeysingId> =
  //              eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId)).getOrThrow()
  //          kontrollLoeysingList.map { getResultatEksternFromRapportLoeysing(it) }
  //        }
  //        .fold(
  //            onSuccess = { ResponseEntity.ok(it) },
  //            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  //  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}")
  fun getResultRapportLoeysing(@PathVariable rapportId: String, @PathVariable loeysingId: Int) {
    return kotlin
        .runCatching { eksternResultatService.getRapportForLoeysing(rapportId, loeysingId) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  //  private fun getResultatEksternFromRapportLoeysing(kontrollLoeysing: KontrollIdLoeysingId) =
  //      resultatOversiktLoeysing(kontrollLoeysing).map { it.toResultatOversiktLoeysingEkstern() }

  @GetMapping("rapport/{rapportId}/tema")
  fun getResultatPrTema(@PathVariable rapportId: String): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrTema(rapportId)
        .fold(
            onSuccess = { resultatTema -> ResponseEntity.ok(resultatTema) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/krav")
  fun getResultatPrKrav(@PathVariable rapportId: String): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrKrav(rapportId)
        .fold(
            onSuccess = { resultatKrav -> ResponseEntity.ok(resultatKrav) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/{suksesskriterium}")
  fun getResultatListKontroll(
      @PathVariable rapportId: String,
      @PathVariable suksesskriterium: String
  ): ResponseEntity<out Any> {

    return kotlin
        .runCatching {
          eksternResultatService.getResultatListKontrollAsEksterntResultat(
              suksesskriterium, rapportId)
        }
        .fold(
            onSuccess = { results -> ResponseEntity.ok(results.flatten()) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @PutMapping("publiser/kontroll/{kontrollId}")
  fun publiserResultat(@PathVariable kontrollId: Int): ResponseEntity<Boolean> {
    logger.debug("Publiserer rapport for kontroll id $kontrollId")

    return try {
      eksternResultatService.publiser(kontrollId)
      ResponseEntity.ok(true)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build<Boolean>()
    } catch (e: Exception) {
      ResponseEntity.badRequest().build<Boolean>()
    }
  }
}
