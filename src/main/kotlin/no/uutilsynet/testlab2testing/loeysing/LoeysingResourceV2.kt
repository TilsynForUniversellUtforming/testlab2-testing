package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

fun locationForId(id: Int): URI = URI("/v2/loeysing/${id}")

@RestController
@RequestMapping("v2/loeysing")
class LoeysingResourceV2(val loeysingDAO: LoeysingDAO, val maalingDAO: MaalingDAO) {

  @PostMapping
  fun createLoeysing(@RequestBody external: Loeysing.External) =
      runCatching {
            val namn = validateNamn(external.namn).getOrThrow()
            val url = URL(external.url)
            val orgnummer = validateOrgNummer(external.orgnummer).getOrThrow()

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
