package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.createWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.dto.Testregel.Companion.validateTestRegel
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/testregler")
class TestregelResource(val testregelDAO: TestregelDAO, val maalingDAO: MaalingDAO) {

  data class CreateTestregelDTO(
      val krav: String,
      val testregelNoekkel: String,
      val kravTilSamsvar: String
  )

  private val locationForId: (Int) -> URI = { id -> URI("/v1/testregler/${id}") }

  @GetMapping("{id}")
  fun getTestregel(@PathVariable id: Int): ResponseEntity<Testregel> =
      testregelDAO.getTestregel(id)?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @GetMapping
  fun getTestregelList(): ResponseEntity<List<Testregel>> =
      ResponseEntity.ok(testregelDAO.getTestregelList())

  @PostMapping
  fun createTestregel(@RequestBody dto: CreateTestregelDTO): ResponseEntity<out Any> =
      createWithErrorHandling(
          {
            validateTestRegel(dto.krav, dto.testregelNoekkel, dto.kravTilSamsvar)
            testregelDAO.createTestregel(dto.krav, dto.testregelNoekkel, dto.kravTilSamsvar)
          },
          locationForId)

  @PutMapping
  fun updateTestregel(@RequestBody testregel: Testregel): ResponseEntity<out Any> =
      executeWithErrorHandling {
        validateTestRegel(testregel.krav, testregel.testregelNoekkel, testregel.kravTilSamsvar)
        testregelDAO.updateTestregel(testregel)
      }

  @DeleteMapping("{testregelId}")
  fun deleteTestregel(@PathVariable("testregelId") testregelId: Int): ResponseEntity<out Any> =
      executeWithErrorHandling {
        val maalingTestregelUsageList = testregelDAO.getMaalingTestregelListById(testregelId)
        if (maalingTestregelUsageList.isNotEmpty()) {
          val testregel =
              testregelDAO.getTestregel(testregelId)
                  ?: throw IllegalArgumentException("Testregel med id $testregelId finnes ikke")
          val maalingList =
              maalingDAO
                  .getMaalingList()
                  .filter { maalingTestregelUsageList.contains(it.id) }
                  .map { it.navn }
          throw IllegalArgumentException(
              "Testregel $testregel er i bruk i følgjande målingar: ${maalingList.joinToString(", ")}")
        }
        testregelDAO.deleteTestregel(testregelId)
      }
}
