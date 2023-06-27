package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import no.uutilsynet.testlab2testing.maaling.validateNamn
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v2/loeysing")
class LoeysingResourceV2(val loeysingDAO: LoeysingDAO, val maalingDAO: MaalingDAO) {

  data class CreateLoeysingDTO(val namn: String, val url: String, val orgnummer: String)

  @PostMapping
  fun createLoeysing(@RequestBody dto: CreateLoeysingDTO) =
      runCatching {
            val namn = validateNamn(dto.namn).getOrThrow()
            val url = URL(dto.url)
            val orgnummer = validateOrgNummer(dto.orgnummer).getOrThrow()

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
