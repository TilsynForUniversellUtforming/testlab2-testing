package no.uutilsynet.testlab2testing.loeysing

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.validation.ClockProvider
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import java.net.URI
import java.util.*
import no.uutilsynet.testlab2testing.loeysing.UtvalResource.NyttUtval
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Clock
import java.time.ZoneId

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class UtvalResourceTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val utvalResource: UtvalResource,
    @Autowired val utvalDAO: UtvalDAO,
) {
  @MockitoSpyBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient
    @MockitoBean
    lateinit var clockProvider: ClockProvider
  val uuid = UUID.randomUUID().toString()

  @LocalServerPort var port: Int = 0

  @BeforeAll
  fun beforeAll() {
      doReturn(loeysingList).`when`(loeysingsRegisterClient).getMany(loeysingList.map { it.id })
      doReturn(loeysingList[0]).`when`(loeysingsRegisterClient).saveLoeysing(loeysingar[0].namn, URI(loeysingar[0].url).toURL(), loeysingar[0].orgnummer)
      doReturn(loeysingList[1]).`when`(loeysingsRegisterClient).saveLoeysing(loeysingar[1].namn, URI(loeysingar[1].url).toURL(), loeysingar[1].orgnummer)
      doReturn(Clock.fixed(maalingDateStart, ZoneId.systemDefault())).`when`(clockProvider).clock

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
    given()
        .port(port)
        .contentType(ContentType.JSON)
        .body(NyttUtval(uuid, loeysingar))
        .post("/v1/utval")
        .then()
        .statusCode(201)
  }

  @Test
  @DisplayName("når vi har laget et utvalg, skal vi kunne hente det ut igjen")
  fun hentUtval() {
    val location =
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(NyttUtval(uuid, loeysingar))
            .post("/v1/utval")
            .then()
            .statusCode(201)
            .extract()
            .header("Location")

    val utval: Utval = restTemplate.getForObject(location, Utval::class.java)

    assertThat(utval.namn).isEqualTo(uuid)
    assertThat(utval.loeysingar.map { it.namn }).containsAll(listOf("UUTilsynet", "Digdir"))
    assertThat(utval.oppretta).isNotNull()
  }

  @DisplayName(
      "når vi opprettar eit utval med ei løysing som ikkje finst i databasen, så skal løysinga bli lagra, og utvalet oppretta")
  @Test
  fun opprettUtvalMedNyLoeysing() {
    val uutilsynet = Loeysing.External("UUTilsynet", "https://www.uutilsynet.no", "991825827")
    val digdir = Loeysing.External("Digdir", "https://www.digdir.no", "991825827")
    val randomLoeysing = Loeysing.External(uuid, "https://www.$uuid.com", "000000000")
    val randomLoeysingNew = Loeysing(3, randomLoeysing.namn, URI(randomLoeysing.url).toURL(), randomLoeysing.orgnummer, uuid)
    val newList = loeysingList + randomLoeysingNew

    doReturn(randomLoeysingNew).`when`(loeysingsRegisterClient).saveLoeysing(randomLoeysing.namn, URI(randomLoeysing.url).toURL(), randomLoeysing.orgnummer)
    doReturn(loeysingList).`when`(loeysingsRegisterClient).getMany(listOf(randomLoeysingNew.id), maalingDateStart)
    doReturn(newList).`when`(loeysingsRegisterClient).getMany(newList.map { it.id }, maalingDateStart)

    val loeysingList = listOf(uutilsynet, digdir, randomLoeysing)
    val location =
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(NyttUtval(uuid, loeysingList))
            .post("/v1/utval")
            .then()
            .statusCode(201)
            .extract()
            .header("Location")

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
    val location =
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(NyttUtval(uuid, loeysingList))
            .post("/v1/utval")
            .then()
            .statusCode(201)
            .extract()
            .header("Location")
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
    given()
        .port(port)
        .contentType(ContentType.JSON)
        .get("/v1/utval")
        .`as`(Array<UtvalListItem>::class.java)
        .forEach {
          assertThat(it.id).isNotNull()
          assertThat(it.namn).isNotBlank()
          assertThat(it.oppretta).isNotNull()
        }
  }

  @DisplayName("vi skal kunne slette eit utval")
  @Test
  fun slettUtval() {
    val location =
        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(NyttUtval(uuid, loeysingar))
            .post("/v1/utval")
            .then()
            .statusCode(201)
            .extract()
            .header("Location")

    given().port(port).delete(location).then().statusCode(200)

    given().port(port).get(location).then().statusCode(404)
  }
}
