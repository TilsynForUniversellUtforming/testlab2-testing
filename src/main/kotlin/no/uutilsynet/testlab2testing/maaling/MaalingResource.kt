package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.dto.Loeysing
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/maalinger")
class MaalingResource(val maalingDAO: MaalingDAO, val crawler: Crawler) {
  data class NyMaalingDTO(val navn: String, val loeysingList: List<Loeysing>)
  class InvalidUrlException(message: String) : Exception(message)

  private val logger = LoggerFactory.getLogger(MaalingResource::class.java)

  @PostMapping
  fun nyMaaling(@RequestBody dto: NyMaalingDTO): ResponseEntity<Any> =
      runCatching {
            val ids = dto.loeysingList.map { it.id }
            val navn = validateNavn(dto.navn).getOrThrow()
            maalingDAO.createMaaling(navn, ids)
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })

  @GetMapping
  fun list(): List<Maaling> {
    return maalingDAO.getMaalingList()
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<Maaling> =
      maalingDAO.getMaaling(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

  @PutMapping("{id}/status")
  fun putNewStatus(
      @PathVariable id: Int,
      @RequestBody data: Map<String, String>
  ): ResponseEntity<Any> {
    return runCatching<ResponseEntity<Any>> {
          val maaling = maalingDAO.getMaaling(id)!!
          val newStatus = validateStatus(data["status"]).getOrThrow()
          when {
            newStatus == Status.Crawling && maaling is Maaling.Planlegging -> {
              val updated = crawler.start(maaling)
              maalingDAO.save(updated).getOrThrow()
              ResponseEntity.ok().build()
            }
            else -> {
              ResponseEntity.badRequest().build()
            }
          }
        }
        .getOrElse { exception ->
          logger.error(exception.message)
          when (exception) {
            is NullPointerException -> ResponseEntity.notFound().build()
            is IllegalArgumentException -> ResponseEntity.badRequest().build()
            else -> ResponseEntity.internalServerError().body(exception.message)
          }
        }
  }

  @GetMapping("loeysingar") fun getLoeysingarList(): List<Loeysing> = maalingDAO.getLoeysingarList()

  private fun handleErrors(exception: Throwable): ResponseEntity<Any> =
      when (exception) {
        is InvalidUrlException,
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }
}
