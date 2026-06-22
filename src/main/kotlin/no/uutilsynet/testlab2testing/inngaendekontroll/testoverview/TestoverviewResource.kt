package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/testoverview")
class TestoverviewResource(val testoverviewService: TestoverviewService) {

  @GetMapping("/kontroll/{kontrollId}")
  fun byKontroll(@PathVariable kontrollId: Int): List<TestingStatus> {
    return testoverviewService.listTestOverviewElements(kontrollId)
  }
}
