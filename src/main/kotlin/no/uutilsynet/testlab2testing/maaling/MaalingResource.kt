package no.uutilsynet.testlab2testing.maaling

import java.net.MalformedURLException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController()
@RequestMapping("v1/maalinger")
class MaalingResource(val maalingDAO: MaalingDAO) {
  data class NyMaalingDTO(val navn: String, val url: String)

  @PostMapping
  fun nyMaaling(@RequestBody dto: NyMaalingDTO): ResponseEntity<Any> =
      runCatching {
            val url = validateURL(dto.url).getOrThrow()
            val navn = validateNavn(dto.navn).getOrThrow()
            maalingDAO.createMaaling(navn, url)
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception, dto) })

  @GetMapping
  fun list(): List<GetMaalingDTO> {
    return maalingDAO.getMaalinger().map { GetMaalingDTO.from(it) }
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

  private fun handleErrors(exception: Throwable, dto: NyMaalingDTO): ResponseEntity<Any> =
      when (exception) {
        is MalformedURLException ->
            ResponseEntity.badRequest().body("${dto.url} er ikkje ei gyldig adresse.")
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }
}

data class GetMaalingDTO(
    val id: Int,
    val navn: String,
    val url: String,
    val status: String,
    val aksjoner: List<Aksjon>
) {
  companion object {
    fun from(maaling: Maaling): GetMaalingDTO {
      return GetMaalingDTO(
          maaling.id,
          maaling.navn,
          maaling.url.toString(),
          Maaling.status(maaling),
          Maaling.aksjoner(maaling))
    }
  }
}
