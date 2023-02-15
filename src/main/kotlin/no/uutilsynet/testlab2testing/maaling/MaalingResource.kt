package no.uutilsynet.testlab2testing.maaling

import java.net.MalformedURLException
import java.net.URI
import java.net.URL
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
              { maalingV1 ->
                ResponseEntity.created(URI.create("/v1/maalinger/${maalingV1.id}")).build()
              },
              { exception -> handleErrors(exception, dto) })

  @GetMapping
  fun list(): List<GetMaalingDTO> {
    return maalingDAO.getMaalinger().map { GetMaalingDTO.from(it) }
  }

  data class GetMaalingDTO(val id: Int, val navn: String, val url: String, val status: String) {
    companion object {
      fun from(maaling: Maaling): GetMaalingDTO {
        val status =
            when (maaling) {
              is Maaling.Planlegging -> "planlegging"
            }
        return GetMaalingDTO(maaling.id, maaling.navn, maaling.url.toString(), status)
      }
    }
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<GetMaalingDTO> =
      maalingDAO.getMaaling(id)?.let { ResponseEntity.ok(GetMaalingDTO.from(it)) }
          ?: ResponseEntity.notFound().build()

  private fun handleErrors(exception: Throwable, dto: NyMaalingDTO): ResponseEntity<Any> =
      when (exception) {
        is MalformedURLException ->
            ResponseEntity.badRequest().body("${dto.url} er ikkje ei gyldig adresse.")
        is NullPointerException -> ResponseEntity.badRequest().body(exception.message)
        else -> ResponseEntity.internalServerError().body(exception.message)
      }
}

fun validateURL(s: String): Result<URL> = runCatching { URL(s) }

fun validateNavn(s: String): Result<String> = runCatching {
  if (s == "") {
    throw IllegalArgumentException("mangler navn")
  } else {
    s
  }
}
