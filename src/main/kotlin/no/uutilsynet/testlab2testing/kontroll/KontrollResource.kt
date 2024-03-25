package no.uutilsynet.testlab2testing.kontroll

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(val kontrollDAO: KontrollDAO) {
  private val logger: Logger = LoggerFactory.getLogger(KontrollResource::class.java)

  @PostMapping
  fun createKontroll(@RequestBody opprettKontroll: OpprettKontroll): ResponseEntity<Unit> {
    return runCatching {
          val id = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
          location(id)
        }
        .fold(
            onSuccess = { location -> ResponseEntity.created(location).build() },
            onFailure = {
              logger.error("Feil ved oppretting av kontroll", it)
              ResponseEntity.badRequest().build()
            })
  }

  @GetMapping("/{id}")
  fun getKontroll(@PathVariable id: Int): ResponseEntity<OpprettetKontroll> {
    return runCatching { kontrollDAO.getKontroll(id).getOrThrow() }
        .fold(
            onSuccess = {
              if (it != null) ResponseEntity.ok(it) else ResponseEntity.notFound().build()
            },
            onFailure = {
              logger.error("Feil ved henting av kontroll", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @DeleteMapping("/{id}")
  fun deleteKontroll(@PathVariable id: Int): ResponseEntity<Unit> {
    return runCatching { kontrollDAO.deleteKontroll(id).getOrThrow() }
        .fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
              logger.error("Feil ved sletting av kontroll", it)
              ResponseEntity.internalServerError().build()
            })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: OpprettetKontroll.Sakstype,
      val arkivreferanse: String,
  )
}
