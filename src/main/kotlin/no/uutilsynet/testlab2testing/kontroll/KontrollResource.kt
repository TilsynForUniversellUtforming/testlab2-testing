package no.uutilsynet.testlab2testing.kontroll

import no.uutilsynet.testlab2testing.kontroll.Kontroll.Testreglar
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/kontroller")
class KontrollResource(
    val kontrollDAO: KontrollDAO,
    val testregelDAO: TestregelDAO,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
) {
  private val logger: Logger = LoggerFactory.getLogger(KontrollResource::class.java)

  data class KontrollListItem(
      val id: Int,
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
      val kontrollType: Kontroll.KontrollType
  )

  @GetMapping
  fun getKontroller(): List<KontrollListItem> {
    return runCatching { kontrollDAO.getKontroller().getOrThrow() }
        .getOrElse {
          logger.error("Feilet da jeg skulle hente alle kontroller", it)
          throw RuntimeException(it)
        }
  }

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
              Kontroll.KontrollType.InngaaendeKontroll,
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
              },
              kontrollDB.sideutval)
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
              is KontrollUpdate.Sideutval -> {
                val (kontroll, sideutvalList) = updateBody
                if (sideutvalList.any { it.begrunnelse.isBlank() }) {
                  logger.error("Ugyldig sideutval for kontroll: ${kontroll.id}")
                  throw IllegalArgumentException("Ugyldige sider i sideutval")
                }
                kontrollDAO.updateKontroll(kontroll, sideutvalList).getOrThrow()
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

  @GetMapping("sideutvaltype")
  fun getSideutvalType(): ResponseEntity<out Any> =
      runCatching { ResponseEntity.ok(kontrollDAO.getSideutvalType()) }
          .getOrElse {
            logger.error("Feila ved henting av sideutvaltyper", it)
            ResponseEntity.internalServerError().body(it.message)
          }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class OpprettKontroll(
      val tittel: String,
      val saksbehandler: String,
      val sakstype: Kontroll.Sakstype,
      val arkivreferanse: String,
  )
}
