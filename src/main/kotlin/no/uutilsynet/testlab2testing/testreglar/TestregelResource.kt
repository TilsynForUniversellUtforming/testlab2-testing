package no.uutilsynet.testlab2testing.testreglar

import no.uutilsynet.testlab2testing.dto.Regelsett
import no.uutilsynet.testlab2testing.dto.Testregel
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/testreglar")
class TestregelResource(val testregelDAO: TestregelDAO) : TestregelApi {

  @GetMapping override fun listTestreglar(): List<Testregel> = testregelDAO.listTestreglar()

  @GetMapping("regelsett")
  override fun listRegelsett(): List<Regelsett> = testregelDAO.listRegelsett()

  @PostMapping
  override fun createTestregel(@RequestBody testregelRequest: TestregelRequest): Int =
      testregelDAO.createTestregel(testregelRequest)

  @PostMapping("regelsett")
  override fun createRegelsett(@RequestBody regelsettRequest: RegelsettRequest): Int =
      testregelDAO.createRegelsett(regelsettRequest)

  @PutMapping
  override fun updateTestregel(@RequestBody testregel: Testregel): Testregel =
      testregelDAO.updateTestregel(testregel)

  @PutMapping("regelsett")
  override fun updateRegelsett(@RequestBody regelsett: Regelsett): Regelsett =
      testregelDAO.updateRegelsett(regelsett)

  @DeleteMapping("{id}")
  override fun deleteTestregel(@PathVariable id: Int) {
    testregelDAO.deleteTestregel(id)
  }

  @DeleteMapping("regelsett/{id}")
  override fun deleteRegelsett(@PathVariable id: Int) {
    testregelDAO.deleteRegelsett(id)
  }
}
