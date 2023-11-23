package no.uutilsynet.testlab2testing.inngaendekontroll

import no.uutilsynet.testlab2testing.common.validateOrgNummer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/saker")
class SakResource(val sakDAO: SakDAO) {
  @PostMapping
  fun createSak(@RequestBody virksomhet: String): ResponseEntity<Unit> {
    return runCatching {
          val orgnummer = validateOrgNummer(virksomhet).getOrThrow()
          val sak = Sak(orgnummer)
          sakDAO.save(sak).getOrThrow()
        }
        .fold(
            onSuccess = { id -> ResponseEntity.created(location(id)).build() },
            onFailure = { ResponseEntity.badRequest().build() })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()

  @GetMapping("/{id}")
  fun getSak(@PathVariable id: Int): ResponseEntity<Sak> {
    return sakDAO
        .getSak(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }
}
