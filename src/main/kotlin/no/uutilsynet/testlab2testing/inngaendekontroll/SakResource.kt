package no.uutilsynet.testlab2testing.inngaendekontroll

import no.uutilsynet.testlab2testing.common.validateOrgNummer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/saker")
class SakResource(val sakDAO: SakDAO) {

  data class NySak(val virksomhet: String)

  @PostMapping
  fun createSak(@RequestBody nySak: NySak): ResponseEntity<Unit> {
    return runCatching {
          val virksomhet = validateOrgNummer(nySak.virksomhet).getOrThrow()
          val sak = Sak(virksomhet)
          sakDAO.save(sak).getOrThrow()
        }
        .fold(
            onSuccess = { id -> ResponseEntity.created(location(id)).build() },
            onFailure = { ResponseEntity.badRequest().build() })
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

  @PutMapping("/{id}")
  fun updateSak(@PathVariable id: Int, @RequestBody sak: Sak): ResponseEntity<Sak> {
    return sakDAO
        .update(id, sak)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }
}
