package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.dto.Loeysing
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController()
@RequestMapping("v1/maalinger")
class MaalingResource(val maalingDAO: MaalingDAO) {
  data class NyMaalingDTO(val navn: String, val loeysingList: List<Loeysing>)
  class InvalidUrlException(message: String) : Exception(message)

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
  fun list(): List<GetMaalingDTO> {
    return maalingDAO.getMaalingList().map { GetMaalingDTO.from(it) }
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<GetMaalingDTO> =
      maalingDAO.getMaaling(id)?.let { ResponseEntity.ok(GetMaalingDTO.from(it)) }
          ?: ResponseEntity.notFound().build()

  @PutMapping("{id}/status")
  fun putNewStatus(
    @PathVariable id: Int,
    @RequestBody data: Map<String, String>
  ): ResponseEntity<Void> {
    return runCatching<ResponseEntity<Void>> {
      val maaling = maalingDAO.getMaaling(id)!!
      val newStatus = validateStatus(data["status"]).getOrThrow()
      val updated = Maaling.updateStatus(maaling, newStatus).getOrThrow()
      maalingDAO.save(updated).getOrThrow()
      ResponseEntity.ok().build()
    }
      .getOrElse { exception ->
        when (exception) {
          is NullPointerException -> ResponseEntity.notFound().build()
          is IllegalArgumentException -> ResponseEntity.badRequest().build()
          else -> ResponseEntity.internalServerError().build()
        }
      }
  }

  private fun handleErrors(exception: Throwable): ResponseEntity<Any> =
      when (exception) {
        is InvalidUrlException,
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }
}

data class GetMaalingDTO(
  val id: Int,
  val navn: String,
  val loeysingList: List<Loeysing>,
  val status: String,
  val aksjoner: List<Aksjon>
) {
  companion object {
    fun from(maaling: Maaling): GetMaalingDTO {
      return GetMaalingDTO(
        maaling.id,
        maaling.navn,
        maaling.loeysingList,
        Maaling.status(maaling),
        Maaling.aksjoner(maaling))
    }
  }
}
