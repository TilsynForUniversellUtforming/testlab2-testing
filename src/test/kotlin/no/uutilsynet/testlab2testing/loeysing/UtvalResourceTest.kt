package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
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
    @Autowired val utvalDAO: UtvalDAO,
) {
  val uuid = UUID.randomUUID().toString()

  @AfterAll
  fun tearDown() {
    utvalDAO.jdbcTemplate.update("delete from utval where namn = :namn", mapOf("namn" to uuid))
    utvalDAO.jdbcTemplate.update("delete from loeysing where namn = :namn", mapOf("namn" to uuid))
  }

  @Test
  @DisplayName("vi skal kunne opprette eit nytt utval")
  fun nyttUtval() {
    val loeysingList =
        listOf(
            Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827"),
            Loeysing.External("Digdir", "https://www.digdir.no", "991825827"))
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingList))
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
  }

  @Test
  @DisplayName("når vi har laget et utvalg, skal vi kunne hente det ut igjen")
  fun hentUtval() {
    val loeysingList =
        listOf(
            Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827"),
            Loeysing.External("Digdir", "https://www.digdir.no", "991825827"))
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingList))
    assert(response.statusCode.is2xxSuccessful)

    val location = response.headers.location
    val utval: Utval = restTemplate.getForObject(location, Utval::class.java)

    assertThat(utval.namn).isEqualTo(uuid)
    assertThat(utval.loeysingar.map { it.namn }).containsAll(listOf("UUTilsynet", "Digdir"))
  }

  @DisplayName(
      "når vi opprettar eit utval med ei løysing som ikkje finst i databasen, så skal løysinga bli lagra, og utvalet oppretta")
  @Test
  fun opprettUtvalMedNyLoeysing() {
    val uutilsynet = Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827")
    val digdir = Loeysing.External("Digdir", "https://www.digdir.no", "991825827")
    val randomLoeysing = Loeysing.External(uuid, "https://www.$uuid.com", "000000000")

    val loeysingList = listOf(uutilsynet, digdir, randomLoeysing)
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingList))
    assert(response.statusCode.is2xxSuccessful)

    val location = response.headers.location
    val utval: Utval = restTemplate.getForObject(location, Utval::class.java)

    assertThat(utval.namn).isEqualTo(uuid)
    assertThat(utval.loeysingar.map { it.namn }).containsAll(listOf("UUTilsynet", "Digdir", uuid))
  }

  @DisplayName(
      "når utvalet har ein URL som manglar protocol, så skal vi anta https, og utvalet skal opprettast")
  @Test
  fun opprettUtvalMedUrlUtenProtocol() {
    val uutilsynet = Loeysing.External("UUTilsynet", "www.uutilsynet.no", "991825827")
    val digdir = Loeysing.External("Digdir", "www.digdir.no", "991825827")

    val loeysingList = listOf(uutilsynet, digdir)
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingList))
    assert(response.statusCode.is2xxSuccessful)

    val location = response.headers.location
    val utval: Utval = restTemplate.getForObject(location, Utval::class.java)

    assertThat(utval.namn).isEqualTo(uuid)
    assertThat(utval.loeysingar.map { it.url })
        .containsAll(listOf(URL("https://www.uutilsynet.no/"), URL("https://www.digdir.no/")))
  }
}
