package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.util.*
import no.uutilsynet.testlab2testing.loeysing.UtvalResource.NyttUtval
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtvalResourceTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val utvalResource: UtvalResource,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient
) {
  val uuid = UUID.randomUUID().toString()

  @BeforeAll
  fun beforeAll() {
    loeysingsRegisterClient
        .saveLoeysing("UUTilsynet", URI("https://www.uutilsynet.no/").toURL(), "991825827")
        .getOrThrow()
    loeysingsRegisterClient
        .saveLoeysing("Digdir", URI("https://www.digdir.no/").toURL(), "991825827")
        .getOrThrow()
  }

  @AfterAll
  fun tearDown() {
    utvalDAO.jdbcTemplate.update("delete from utval where namn = :namn", mapOf("namn" to uuid))
    loeysingsRegisterClient
        .search(uuid)
        .mapCatching { loeysingar ->
          loeysingar.forEach { loeysingsRegisterClient.delete(it.id).getOrThrow() }
        }
        .getOrThrow()
  }

  private val loeysingar =
      listOf(
          Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827"),
          Loeysing.External("Digdir", "https://www.digdir.no", "991825827"))

  @Test
  @DisplayName("vi skal kunne opprette eit nytt utval")
  fun nyttUtval() {
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingar))
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
  }

  @Test
  @DisplayName("når vi har laget et utvalg, skal vi kunne hente det ut igjen")
  fun hentUtval() {
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingar))
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
        .containsAll(
            listOf(
                URI("https://www.uutilsynet.no/").toURL(), URI("https://www.digdir.no/").toURL()))
  }

  @DisplayName("vi skal kunne hente ei liste med alle utval")
  @Test
  fun hentAlleUtval() {
    val response = restTemplate.getForEntity("/v1/utval", Array<UtvalListItem>::class.java)
    assertThat(response.statusCode.is2xxSuccessful).isTrue()
    response.body!!.forEach {
      assertThat(it.id).isNotNull()
      assertThat(it.namn).isNotBlank()
    }
  }

  @DisplayName("vi skal kunne slette eit utval")
  @Test
  fun slettUtval() {
    val response: ResponseEntity<Unit> = utvalResource.createUtval(NyttUtval(uuid, loeysingar))
    assert(response.statusCode.is2xxSuccessful)

    val location = response.headers.location

    val deleteResponse = restTemplate.exchange(location, HttpMethod.DELETE, null, Unit::class.java)
    assertThat(deleteResponse.statusCode.is2xxSuccessful).isTrue()

    val getResponse = restTemplate.getForEntity(location, Utval::class.java)
    assertThat(getResponse.statusCode.is2xxSuccessful).isFalse()
  }
}
