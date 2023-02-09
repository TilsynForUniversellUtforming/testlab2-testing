package no.uutilsynet.testlab2testing.testreglar

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TestreglarIntegrationtest(@Autowired val testregelDAO: TestregelDAO) {

  @Test
  fun listTestreglar() {
    assertDoesNotThrow { testregelDAO.listTestreglar() }
  }

  @Test
  fun listRegelsett() {
    assertDoesNotThrow { testregelDAO.listRegelsett() }
  }
}
