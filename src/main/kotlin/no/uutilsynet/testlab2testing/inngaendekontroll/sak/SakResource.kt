package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import no.uutilsynet.testlab2testing.common.validateOrgNummer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/saker")
class SakResource(val sakDAO: SakDAO) {

  val logger: Logger = LoggerFactory.getLogger(SakResource::class.java)

  data class NySak(val virksomhet: String)

  @PostMapping
  fun createSak(@RequestBody nySak: NySak): ResponseEntity<Unit> {
    return runCatching {
          val virksomhet = validateOrgNummer(nySak.virksomhet).getOrThrow()
          sakDAO.save(virksomhet).getOrThrow()
        }
        .fold(
            onSuccess = { id -> ResponseEntity.created(location(id)).build() },
            onFailure = {
              logger.error("Feil ved oppretting av sak", it)
              ResponseEntity.badRequest().build()
            })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  @GetMapping("/{id}")
  fun getSak(@PathVariable id: Int): ResponseEntity<Sak> {
    return sakDAO
        .getSak(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @GetMapping
  fun getAlleSaker(): ResponseEntity<List<SakListeElement>> {
    return ResponseEntity.ok(sakDAO.getAlleSaker())
  }

  @PutMapping("/{id}")
  fun updateSak(@PathVariable id: Int, @RequestBody sak: Sak): ResponseEntity<Sak> {
    require(sak.id == id) { "id i URL-en og id er ikkje den same" }
    return sakDAO
        .update(sak)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }
}
