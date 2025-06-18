package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
    @Autowired val eksternResultatService: EksternResultatService,
    @Autowired val publiseringService: EksternResultatPubliseringService,
    @Autowired val bildeService: BildeService
) {

  private val logger = LoggerFactory.getLogger(EksternResultatResource::class.java)

  @GetMapping
  fun findTestForOrgNr(
      @RequestParam("orgnr") orgnr: String?,
      @RequestParam("searchparam") searchparam: String?
  ): ResponseEntity<Any?> {
    logger.debug("Henter tester for orgnr $orgnr")

    return try {
      val result = findTests(searchparam, orgnr)
      ResponseEntity.ok(result)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build()
    } catch (e: IllegalArgumentException) {
      logger.warn(e.message)
      ResponseEntity.status(HttpStatusCode.valueOf(422)).body(e.message)
    } catch (e: Exception) {
      logger.error(e.message)
      ResponseEntity.badRequest().build()
    }
  }

  private fun findTests(searchparam: String?, orgnr: String?): TestListElementEkstern {
    return if (searchparam != null) {
      eksternResultatService.findTestForOrgNr(searchparam).getOrThrow()
    } else if (orgnr != null) {
      eksternResultatService.findTestForOrgNr(orgnr).getOrThrow()
    } else {
      throw IllegalArgumentException("Mangler søkeparameter")
    }
  }

  @GetMapping("rapport/{rapportId}")
  fun getRestultatRapport(@PathVariable rapportId: String): ResponseEntity<out Any> {
    return kotlin
        .runCatching { eksternResultatService.getResultatForRapport(rapportId) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}")
  fun getResultRapportLoeysing(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int
  ): ResponseEntity<out Any> {
    return kotlin
        .runCatching { eksternResultatService.getRapportForLoeysing(rapportId, loeysingId) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/tema")
  fun getResultatPrTema(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int
  ): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrTema(rapportId, loeysingId)
        .fold(
            onSuccess = { resultatTema -> ResponseEntity.ok(resultatTema) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/krav")
  fun getResultatPrKrav(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int
  ): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrKrav(rapportId, loeysingId)
        .fold(
            onSuccess = { resultatKrav -> ResponseEntity.ok(resultatKrav) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/testregel/{testregelId}")
  fun getDetaljertResultat(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
      @PathVariable testregelId: Int
  ): ResponseEntity<out Any> {

    return kotlin
        .runCatching {
          eksternResultatService.getResultatListKontrollAsEksterntResultat(
              rapportId, loeysingId, testregelId)
        }
        .fold(
            onSuccess = { results -> ResponseEntity.ok(results) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @PutMapping("publiser/kontroll/{kontrollId}")
  fun publiserResultat(@PathVariable kontrollId: Int): ResponseEntity<Boolean> {
    logger.debug("Publiserer rapport for kontroll id $kontrollId")

    return try {
      publiseringService.publiser(kontrollId)
      ResponseEntity.ok(true)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build<Boolean>()
    } catch (e: Exception) {
      ResponseEntity.badRequest().build<Boolean>()
    }
  }

  @GetMapping("sti")
  fun getBilde(@RequestParam("bildesti") bildesti: String): ResponseEntity<InputStreamResource> {

    if (!bildeService.erBildePublisert(bildesti)) {
      return ResponseEntity.status(HttpStatusCode.valueOf(404)).build()
    }

    return bildeService.getBildeResponse(bildesti)
  }
}
