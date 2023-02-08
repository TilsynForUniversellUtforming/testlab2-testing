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
  fun list(): List<MaalingV1> {
    return maalingDAO.getMaalinger()
  }

  @GetMapping("{id}")
  fun getMaaling(@PathVariable id: Int): ResponseEntity<MaalingV1> =
      maalingDAO.getMaaling(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

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
