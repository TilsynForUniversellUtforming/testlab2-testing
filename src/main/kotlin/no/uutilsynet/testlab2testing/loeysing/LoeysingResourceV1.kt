package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

fun locationForId(id: Int): URI = URI("/v1/loeysing/${id}")

@RestController
@RequestMapping("v1/loeysing")
@Deprecated("Erstattet av V2")
class LoeysingResourceV1(val loeysingDAO: LoeysingDAO, val maalingDAO: MaalingDAO) {

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
