package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2testing.kontroll.Kontroll.Testreglar
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    val kontrollDAO: KontrollDAO,
    val testregelDAO: TestregelDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
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
          Kontroll(
              kontrollDB.id,
              Kontroll.KontrollType.ManuellKontroll,
              kontrollDB.tittel,
              kontrollDB.saksbehandler,
              Kontroll.Sakstype.valueOf(kontrollDB.sakstype),
              kontrollDB.arkivreferanse,
              kontrollDB.utval?.let { utval ->
                val idList = utval.loeysingar.map { it.id }
                val loeysingar = loeysingsRegisterClient.getMany(idList).getOrThrow()

                Utval(utval.id, utval.namn, loeysingar, utval.oppretta)
              },
              kontrollDB.testreglar?.let { testreglar ->
                val testregelList = testregelDAO.getMany(testreglar.testregelIdList)
                Testreglar(testreglar.regelsettId, testregelList)
              })
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
      @RequestBody updateBody: KontrollUpdate
  ): ResponseEntity<Unit> =
      runCatching {
            require(updateBody.kontroll.id == id) { "id i URL-en og id er ikkje den same" }
            when (updateBody) {
              is KontrollUpdate.Utval -> {
                val (kontroll, utvalId) = updateBody
                kontrollDAO.updateKontroll(kontroll, utvalId).getOrThrow()
              }
              is KontrollUpdate.Testreglar -> {
                val (kontroll, testreglar) = updateBody
                val (regelsettId, testregelIdList) = testreglar
                kontrollDAO.updateKontroll(kontroll, regelsettId, testregelIdList).getOrThrow()
              }
            }
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

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
  )
}
