package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("v2/loeysing")
class LoeysingResource(
    val maalingDAO: MaalingDAO,
    val loeysingsRegisterProperties: LoeysingsRegisterProperties,
    val loeysingsRegisterClient: LoeysingsRegisterClient
) {
  val logger: Logger = LoggerFactory.getLogger(LoeysingResource::class.java)

  @PostMapping
  fun createLoeysing(
      @RequestBody external: Loeysing.External
  ): ResponseEntity<Map<String, String>> {
    val uri =
        UriComponentsBuilder.fromUriString(loeysingsRegisterProperties.host)
            .pathSegment("v1", "loeysing")
            .build()
            .toUri()
    return ResponseEntity.status(308)
        .location(uri)
        .body(
            mapOf(
                "namn" to external.namn, "url" to external.url, "orgnummer" to external.orgnummer))
  }

  @PutMapping
  fun updateLoeysing(@RequestBody loeysing: Loeysing): ResponseEntity<out Any> =
      runCatching {
            validateNamn(loeysing.namn).getOrThrow()
            val orgnummer = validateOrgNummer(loeysing.orgnummer).getOrThrow()
            val sammeOrgnummer = loeysingsRegisterClient.search(orgnummer).getOrThrow()
            val foundLoeysing = sammeOrgnummer.find { sameURL(it.url, loeysing.url) }
            if (foundLoeysing != null && foundLoeysing.id != loeysing.id) {
              logger.error("Løysing med id ${loeysing.id} er duplikat")
              throw IllegalArgumentException(
                  "Løysing med url ${loeysing.url} og orgnr $orgnummer finnes allereie")
            }
            ResponseEntity.status(308)
                .location(URI("${loeysingsRegisterProperties.host}/v1/loeysing"))
                .body(loeysing)
          }
          .getOrElse {
            when (it) {
              is IllegalArgumentException -> ResponseEntity.badRequest().body(it.message)
              else -> ResponseEntity.internalServerError().body(it.message)
            }
          }

  @GetMapping("{id}")
  fun getLoeysing(@PathVariable id: Int): ResponseEntity<Loeysing> =
      ResponseEntity.status(301)
          .location(URI("${loeysingsRegisterProperties.host}/v1/loeysing/$id"))
          .build()

  @GetMapping
  fun getLoeysingList(
      @RequestParam("namn", required = false) namn: String?,
      @RequestParam("orgnummer", required = false) orgnummer: String?
  ): ResponseEntity<Any> =
      if (namn != null && orgnummer != null) {
        ResponseEntity.badRequest().body("Må søke med enten namn eller orgnummer")
      } else if (namn != null || orgnummer != null) {
        val uri =
            UriComponentsBuilder.fromUriString("${loeysingsRegisterProperties.host}/v1/loeysing")
                .queryParam("search", namn ?: orgnummer)
                .build()
                .toUri()
        ResponseEntity.status(301).location(uri).build()
      } else {
        ResponseEntity.status(301)
            .location(URI("${loeysingsRegisterProperties.host}/v1/loeysing"))
            .build()
      }

  @DeleteMapping("{id}")
  fun deleteLoeysing(@PathVariable("id") loeysingId: Int): ResponseEntity<Any> =
      runCatching {
            val maalingLoeysingUsageList = maalingDAO.findMaalingarByLoeysing(loeysingId)
            if (maalingLoeysingUsageList.isNotEmpty()) {
              val maalingList =
                  maalingDAO
                      .getMaalingList()
                      .filter { maalingLoeysingUsageList.contains(it.id) }
                      .map { it.navn }
              throw IllegalArgumentException(
                  "Løysing $loeysingId er i bruk i følgjande målingar: ${maalingList.joinToString(", ")}")
            }
            ResponseEntity.status(308)
                .location(URI("${loeysingsRegisterProperties.host}/v1/loeysing/$loeysingId"))
                .build<Any>()
          }
          .getOrElse { ex ->
            when (ex) {
              is NullPointerException -> ResponseEntity.badRequest().body(ex.message)
              is IllegalArgumentException -> ResponseEntity.badRequest().body(ex.message)
              else -> ResponseEntity.internalServerError().body(ex.message)
            }
          }
}
