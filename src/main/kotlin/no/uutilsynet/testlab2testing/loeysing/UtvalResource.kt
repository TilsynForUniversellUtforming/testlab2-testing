package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import no.uutilsynet.testlab2testing.maaling.validateIdList
import no.uutilsynet.testlab2testing.maaling.validateNavn
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/utval")
class UtvalResource(@Autowired val utvalDAO: UtvalDAO, @Autowired val loeysingDAO: LoeysingDAO) {
  val logger: Logger = LoggerFactory.getLogger(UtvalResource::class.java)

  data class NyttUtval(val namn: String, val loeysingar: List<Int>)

  @PostMapping
  fun createUtval(@RequestBody nyttUtval: NyttUtval): ResponseEntity<Any> =
      runCatching {
            val namn = validateNavn(nyttUtval.namn).getOrThrow()
            val loeysingIdList = loeysingDAO.getLoeysingIdList()
            val loeysingar =
                validateIdList(nyttUtval.loeysingar, loeysingIdList, "loeysingar").getOrThrow()
            val utvalId = utvalDAO.createUtval(namn, loeysingar).getOrThrow()
            return ResponseEntity.created(URI.create("/v1/utval/$utvalId")).build()
          }
          .getOrElse {
            when (it) {
              is IllegalArgumentException -> ResponseEntity.badRequest().body(it.message)
              else -> ResponseEntity.status(500).body(it.message)
            }
          }

  @GetMapping
  fun getUtvalList() =
      runCatching {
            val utvalList = utvalDAO.getUtvalList().getOrThrow()
            ResponseEntity.ok().body(utvalList)
          }
          .getOrElse {
            logger.atError().log("Klarte ikkje å hente alle utval", it)
            ResponseEntity.internalServerError().build<Unit>()
          }

  @GetMapping("{id}")
  fun getUtval(@PathVariable id: Int): ResponseEntity<Utval> =
      utvalDAO
          .getUtval(id)
          .map { ResponseEntity.ok(it) }
          .getOrElse { ResponseEntity.notFound().build() }

  @DeleteMapping("{id}")
  fun deleteUtval(@PathVariable id: Int) =
      runCatching { utvalDAO.deleteUtval(id).getOrThrow() }
          .getOrElse {
            logger.atError().log("Klarte ikkje å slette utval med id $id", it)
            ResponseEntity.internalServerError().build<Unit>()
          }
}
