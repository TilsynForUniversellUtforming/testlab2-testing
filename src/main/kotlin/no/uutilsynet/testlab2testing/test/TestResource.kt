package no.uutilsynet.testlab2testing.test

import no.uutilsynet.testlab2testing.maaling.AutoTesterAdapter
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("v1/tester")
class TestResource(val maalingDAO: MaalingDAO, val autoTesterAdapter: AutoTesterAdapter) {
  data class TestInput(val maalingId: Int)

  @PostMapping
  fun createNewTest(
    @RequestBody testInput: TestInput
  ): ResponseEntity<AutoTesterAdapter.AutoTesterResponse> {
    val maaling = maalingDAO.getMaaling(testInput.maalingId)
    if (maaling == null) return ResponseEntity.notFound().build()
    return autoTesterAdapter
      .runTests(listOf(maaling.url))
      .fold(
        { response -> ResponseEntity.ok(response) },
        { ResponseEntity.internalServerError().build() })
  }
}
