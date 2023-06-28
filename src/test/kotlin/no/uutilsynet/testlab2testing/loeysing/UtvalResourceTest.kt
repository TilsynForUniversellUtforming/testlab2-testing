package no.uutilsynet.testlab2testing.loeysing

import java.util.*
import no.uutilsynet.testlab2testing.loeysing.UtvalResource.NyttUtval
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtvalResourceTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val utvalResource: UtvalResource,
    @Autowired val loeysingResourceV2: LoeysingResourceV2,
    @Autowired val utvalDAO: UtvalDAO,
) {
  val namn = UUID.randomUUID().toString()

  @AfterAll
  fun slettAlleUtval() {
    utvalDAO.jdbcTemplate.update("delete from utval where namn = :namn", mapOf("namn" to namn))
  }

  @Test
  @DisplayName("vi skal kunne opprette eit nytt utval")
  fun nyttUtval() {
    val loeysingList =
        listOf(
            Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827"),
            Loeysing.External("Digdir", "https://www.digdir.no", "991825827"))
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(namn, loeysingList))
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
  }

  @Test
  @DisplayName("når vi har laget et utvalg, skal vi kunne hente det ut igjen")
  fun hentUtval() {
    val loeysingList =
        listOf(
            Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827"),
            Loeysing.External("Digdir", "https://www.digdir.no", "991825827"))
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(namn, loeysingList))
    assert(response.statusCode.is2xxSuccessful)

    val location = response.headers.location
    val utval: Utval = restTemplate.getForObject(location, Utval::class.java)

    assertThat(utval.namn).isEqualTo(namn)
    assertThat(utval.loeysingar.map { it.namn }).containsAll(listOf("UUTilsynet", "Digdir"))
  }

  // test med løsninger som ikke finnes i databasen
}
