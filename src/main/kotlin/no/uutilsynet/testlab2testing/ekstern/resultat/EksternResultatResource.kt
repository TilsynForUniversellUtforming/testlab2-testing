package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.resultat.ResultatService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
    @Autowired val eksternResultatDAO: EksternResultatDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient,
    @Autowired val resultatService: ResultatService,
    @Autowired val kravregisterClient: KravregisterClient,
    @Autowired val eksternResultatService: EksternResultatService
) {

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
      ResponseEntity.badRequest().build<TestListElementEkstern>()
    }
  }

  @GetMapping("{rapportId}")
  fun getResultatRapport(
      @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatOversiktLoeysingEkstern>> {
    val kontrollLoeysing =
        eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId))
            ?: return ResponseEntity.badRequest().build()

    val results =
        resultatService.getKontrollLoeysingResultatIkkjeRetest(
            kontrollLoeysing.kontrollId, kontrollLoeysing.loeysingId)
    if (results.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }

    return ResponseEntity.ok(results.map { it.toResultatOversiktLoeysingEkstern() })
  }

  @GetMapping("{rapportId}/tema")
  fun getResultatPrTema(
      @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatTemaEkstern>> {

    val kontrollLoeysing =
        eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId))
            ?: return ResponseEntity.badRequest().build()

    val resultatTema =
        resultatService.getResultatPrTema(kontrollLoeysing.kontrollId, null, null, null)

    if (resultatTema.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val resultatTemaEkstern = resultatTema.map { it.toResultatTemaEkstern() }

    return ResponseEntity.ok(resultatTemaEkstern)
  }

  @GetMapping("{rapportId}/krav")
  fun getResultatPrKrav(
      @PathVariable rapportId: String
  ): ResponseEntity<List<ResultatKravEkstern>> {
    val kontrollLoeysing =
        eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId))
            ?: return ResponseEntity.badRequest().build()

    val resultatKrav =
        resultatService.getResultatPrKrav(kontrollLoeysing.kontrollId, null, null, null)
    if (resultatKrav.isEmpty()) {
      return ResponseEntity.badRequest().build()
    }
    val resultatTemaEkstern = resultatKrav.map { it.toResultatKravEkstern() }

    return ResponseEntity.ok(resultatTemaEkstern)
  }

  @GetMapping("{rapportId}/{suksesskriterium}")
  fun getResultatListKontroll(
      @PathVariable rapportId: String,
      @PathVariable suksesskriterium: String
  ): ResponseEntity<List<TestresultatDetaljertEkstern>> {
    val testgrunnlagLoeysing =
        eksternResultatDAO.findKontrollLoeysingFromRapportId((rapportId))
            ?: return ResponseEntity.badRequest().build()

    if (!Regex("""^\d+\.\d+\.\d+$""").matches(suksesskriterium)) {
      return ResponseEntity.badRequest().build()
    }

    val krav =
        runCatching { kravregisterClient.getKrav(suksesskriterium) }
            .getOrElse {
              return ResponseEntity.badRequest().build()
            }

    val results =
        resultatService
            .getResultatListTestgrunnlag(
                testgrunnlagLoeysing.kontrollId, testgrunnlagLoeysing.loeysingId, krav.id)
            .map { it.toTestresultatDetaljertEkstern() }

    return ResponseEntity.ok(results)
  }

  @PutMapping("publiser/kontroll/{kontrollId}")
  fun publiserResultat(@PathVariable kontrollId: Int): ResponseEntity<Boolean> {
    logger.debug("Publiserer rapport for kontroll id $kontrollId")

    val erPublisert = eksternResultatService.erKontrollPublisert(kontrollId)

    return try {
      if (erPublisert) {
        eksternResultatService.avpubliserResultat(kontrollId)
      } else {
        eksternResultatService.publiserResultat(kontrollId)
      }
      ResponseEntity.ok(true)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build<Boolean>()
    } catch (e: Exception) {
      ResponseEntity.badRequest().build<Boolean>()
    }
  }
}
