package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
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

fun locationForId(id: Int): URI = URI("/v2/loeysing/${id}")

@RestController
@RequestMapping("v2/loeysing")
class LoeysingResource(val loeysingDAO: LoeysingDAO, val maalingDAO: MaalingDAO) {
  val logger = LoggerFactory.getLogger(LoeysingResource::class.java)

  @PostMapping
  fun createLoeysing(@RequestBody external: Loeysing.External) =
      runCatching {
            val namn = validateNamn(external.namn).getOrThrow()
            val url = URL(external.url)
            val orgnummer = validateOrgNummer(external.orgnummer).getOrThrow()

            val foundLoeysing = loeysingDAO.findLoeysingByURLAndOrgnummer(url, orgnummer)
            if (foundLoeysing != null) {
              logger.error("Løysing med url $url og orgnr $orgnummer er duplikat")
              throw IllegalArgumentException(
                  "Løysing med url $url og orgnr $orgnummer finnes allereie")
            }

            loeysingDAO.createLoeysing(namn, url, orgnummer)
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })

  @PutMapping
  fun updateLoeysing(@RequestBody loeysing: Loeysing) = executeWithErrorHandling {
    val namn = validateNamn(loeysing.namn).getOrThrow()
    val orgnummer = validateOrgNummer(loeysing.orgnummer).getOrThrow()
    val foundLoeysing = loeysingDAO.findLoeysingByURLAndOrgnummer(loeysing.url, orgnummer)
    if (foundLoeysing != null && foundLoeysing.id != loeysing.id) {
      logger.error("Løysing med id ${loeysing.id} er duplikat")
      throw IllegalArgumentException(
          "Løysing med url ${loeysing.url} og orgnr $orgnummer finnes allereie")
    }
    loeysingDAO.updateLoeysing(Loeysing(loeysing.id, namn, loeysing.url, orgnummer))
  }

  @GetMapping("{id}")
  fun getLoeysing(@PathVariable id: Int): ResponseEntity<Loeysing> =
      loeysingDAO.getLoeysing(id)?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @GetMapping
  fun getLoeysingList(): ResponseEntity<List<Loeysing>> =
      ResponseEntity.ok(loeysingDAO.getLoeysingList())

  @DeleteMapping("{id}")
  fun deleteLoeysing(@PathVariable("id") id: Int) = executeWithErrorHandling {
    val maalingLoeysingUsageList = loeysingDAO.getMaalingLoeysingListById(id)
    if (maalingLoeysingUsageList.isNotEmpty()) {
      val loeysing = loeysingDAO.getLoeysing(id)
      val maalingList =
          maalingDAO
              .getMaalingList()
              .filter { maalingLoeysingUsageList.contains(it.id) }
              .map { it.navn }
      throw IllegalArgumentException(
          "Løysing ${loeysing?.namn ?: "ukjend løysing"} er i bruk i følgjande målingar: ${maalingList.joinToString(", ")}")
    }
    loeysingDAO.deleteLoeysing(id)
  }
}
