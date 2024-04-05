package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.UtvalResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    val kontrollDAO: KontrollDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val utvalResource: UtvalResource
) {
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
  fun getKontroll(@PathVariable id: Int): ResponseEntity<Kontroll> {
    return runCatching {
          val kontrollDB = kontrollDAO.getKontroll(id).getOrThrow()
          val loeysingar =
              loeysingsRegisterClient.getMany(kontrollDB.loeysingar.map { it.id }).getOrThrow()
          val utval = kontrollDB.utvalId?.let { utvalResource.fetchUtval(it).getOrThrow() }
          Kontroll(
              kontrollDB.id,
              Kontroll.KontrollType.ManuellKontroll,
              kontrollDB.tittel,
              kontrollDB.saksbehandler,
              Kontroll.Sakstype.valueOf(kontrollDB.sakstype),
              kontrollDB.arkivreferanse,
              loeysingar,
              utval)
        }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = {
              when (it) {
                is IllegalArgumentException -> ResponseEntity.notFound().build()
                else -> {
                  logger.error("Feil ved henting av kontroll", it)
                  ResponseEntity.internalServerError().build()
                }
              }
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

  @PutMapping("/{id}")
  fun updateKontroll(
      @PathVariable id: Int,
      @RequestBody updateBody: UpdateBody
  ): ResponseEntity<Unit> {
    val kontroll = updateBody.kontroll

    return runCatching {
          require(kontroll.id == id) { "id i URL-en og id er ikkje den same" }
          kontroll.loeysingar.forEach {
            loeysingsRegisterClient.saveLoeysing(it.namn, it.url, it.orgnummer).getOrThrow()
          }
          kontrollDAO.updateKontroll(kontroll).getOrThrow()
        }
        .fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
              when (it) {
                is IllegalArgumentException -> ResponseEntity.badRequest().build()
                else -> {
                  logger.error("Feil ved oppdatering av kontroll", it)
                  ResponseEntity.internalServerError().build()
                }
              }
            })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
  )

  data class UpdateBody(val kontroll: Kontroll)
}
