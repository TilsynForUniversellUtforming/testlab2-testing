package no.uutilsynet.testlab2testing.kontroll

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import io.restassured.parsing.Parser
import io.restassured.path.json.JsonPath
import io.restassured.path.json.JsonPath.from
import java.time.Instant
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalResource
import no.uutilsynet.testlab2testing.regelsett.Regelsett
import no.uutilsynet.testlab2testing.regelsett.RegelsettCreate
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelInit
import no.uutilsynet.testlab2testing.testregel.TestregelInnholdstype
import no.uutilsynet.testlab2testing.testregel.TestregelModus
import no.uutilsynet.testlab2testing.testregel.TestregelStatus
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@DisplayName("KontrollResource")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KontrollResourceTest {
  @LocalServerPort var port: Int = 0

  val kontrollInitBody =
      mapOf(
          "kontrolltype" to "manuell-kontroll",
          "tittel" to "testkontroll",
          "saksbehandler" to "Ola Nordmann",
          "sakstype" to "forvaltningssak",
          "arkivreferanse" to "1234")

  @Test
  @DisplayName("når vi oppretter en kontroll så skal vi få en URI som resultat i location")
  fun createKontroll() {
    val body = kontrollInitBody
    given()
        .port(port)
        .body(body)
        .contentType("application/json")
        .post("/kontroller")
        .then()
        .statusCode(equalTo(201))
        .header("Location", startsWith("http://localhost:$port/kontroller/"))
  }

  @Test
  @DisplayName("gitt at vi har opprettet en kontroll, så skal vi kunne slette den")
  fun deleteKontroll() {
    val body = kontrollInitBody
    val location =
        given()
            .port(port)
            .body(body)
            .contentType("application/json")
            .post("/kontroller")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    given().port(port).delete(location).then().statusCode(equalTo(204))
  }

  @Test
  @DisplayName(
      "gitt at vi har opprettet en kontroll, så skal vi kunne hente den ut igjen med url-en i location")
  fun getKontrollById() {
    val body = kontrollInitBody
    val location =
        given()
            .port(port)
            .body(body)
            .contentType("application/json")
            .post("/kontroller")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    val responseBody = get(location).asString()
    val json: JsonPath = from(responseBody)

    assertThat(json.get<String>("tittel")).isEqualTo("testkontroll")
    assertThat(json.get<String>("saksbehandler")).isEqualTo("Ola Nordmann")
    assertThat(json.get<String>("sakstype")).isEqualTo("forvaltningssak")
    assertThat(json.get<String>("arkivreferanse")).isEqualTo("1234")
    assertThat(json.get<Utval>("utval")).isNull()
  }

  @Test
  @DisplayName(
      "gitt vi har en kontroll, når vi oppdaterer den med et utvalg, så skal kontrollen være lagret med dataene fra utvalget")
  fun updateKontrollWithLoeysingar() {
    RestAssured.defaultParser = Parser.JSON
    val body = kontrollInitBody
    val location =
        given()
            .port(port)
            .body(body)
            .contentType("application/json")
            .post("/kontroller")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    val opprettetKontroll = get(location).`as`(Kontroll::class.java)

    val loeysingar =
        listOf(Loeysing.External("UUTilsynet", "https://www.uutilsynet.no/", "991825827"))
    val nyttUtval = UtvalResource.NyttUtval("testutval", loeysingar)
    val utvalLocation =
        given()
            .port(port)
            .body(nyttUtval)
            .contentType("application/json")
            .post("/v1/utval")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    val utval = get(utvalLocation).`as`(Utval::class.java)

    val updateBody =
        mapOf(
            "kontroll" to opprettetKontroll,
            "utvalId" to utval.id,
            "kontrollSteg" to KontrollSteg.Utval)
    given()
        .port(port)
        .body(updateBody)
        .contentType("application/json")
        .put(location)
        .then()
        .statusCode(equalTo(204))
    val lagretKontroll = get(location).`as`(Kontroll::class.java)

    assertThat(lagretKontroll.utval?.id).isEqualTo(utval.id)
    assertThat(lagretKontroll.utval?.namn).isEqualTo(utval.namn)
    assertThat(lagretKontroll.utval?.loeysingar).isEqualTo(utval.loeysingar)
  }

  @Test
  @DisplayName(
      "gitt at vi har opprettet en kontroll, så skal vi kunne oppdatere den flere ganger med samme utval")
  fun oppdaterUtvalgFlereGanger() {
    RestAssured.defaultParser = Parser.JSON
    val body = kontrollInitBody
    val location =
        given()
            .port(port)
            .body(body)
            .contentType("application/json")
            .post("/kontroller")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    val opprettetKontroll = get(location).`as`(Kontroll::class.java)

    val loeysingar =
        listOf(Loeysing.External("UUTilsynet", "https://www.uutilsynet.no/", "991825827"))
    val nyttUtval = UtvalResource.NyttUtval("testutval", loeysingar)
    val utvalLocation =
        given()
            .port(port)
            .body(nyttUtval)
            .contentType("application/json")
            .post("/v1/utval")
            .then()
            .statusCode(equalTo(201))
            .extract()
            .header("Location")
    val utval = get(utvalLocation).`as`(Utval::class.java)

    val updateBody =
        mapOf(
            "kontroll" to opprettetKontroll,
            "utvalId" to utval.id,
            "kontrollSteg" to KontrollSteg.Utval)
    for (i in 1..3) {
      given()
          .port(port)
          .body(updateBody)
          .contentType("application/json")
          .put(location)
          .then()
          .statusCode(equalTo(204))
    }
  }

  @Nested
  @DisplayName("Hvis det finnes en testregel i databasen")
  inner class DatabaseHasAtLeastOneTestregel {
    private val testregelLocationForId = createDefaultTestregel()

    @Test
    @DisplayName("gitt vi har en kontroll, så skal vi kunne oppdatere testreglar med regelsett")
    fun updateKontrollWithTestreglarRegelsett() {
      RestAssured.defaultParser = Parser.JSON
      val body = kontrollInitBody

      /* Create default kontroll */
      val location =
          given()
              .port(port)
              .body(body)
              .contentType("application/json")
              .post("/kontroller")
              .then()
              .statusCode(equalTo(201))
              .extract()
              .header("Location")
      val opprettetKontroll = get(location).`as`(Kontroll::class.java)

      val testregel = get(testregelLocationForId).`as`(Testregel::class.java)

      /* Create regelsett */
      val nyttRegelsett =
          RegelsettCreate(
              namn = "regelsett_skal_slettes",
              modus = TestregelModus.manuell,
              standard = false,
              testregelIdList = listOf(testregel.id))

      val regelsettLocationForId =
          given()
              .port(port)
              .body(nyttRegelsett)
              .contentType("application/json")
              .post("/v1/regelsett")
              .then()
              .statusCode(equalTo(201))
              .extract()
              .header("Location")
      val regelsett =
          get("http://localhost:$port$regelsettLocationForId").`as`(Regelsett::class.java)

      val updateBody =
          mapOf(
              "kontroll" to opprettetKontroll,
              "testreglar" to
                  mapOf(
                      "regelsettId" to regelsett.id,
                      "testregelIdList" to regelsett.testregelList.map { it.id }),
              "kontrollSteg" to KontrollSteg.Testreglar)
      given()
          .port(port)
          .body(updateBody)
          .contentType("application/json")
          .put(location)
          .then()
          .statusCode(equalTo(204))
      val lagretKontroll = get(location).`as`(Kontroll::class.java)

      assertThat(lagretKontroll.testreglar?.regelsettId).isEqualTo(regelsett.id)
      assertThat(lagretKontroll.testreglar?.testregelList?.map { it.id })
          .isEqualTo(regelsett.testregelList.map { it.id })
    }

    @Test
    @DisplayName("gitt vi har en kontroll, så skal vi kunne legge inn egenvalgte testregler")
    fun updateKontrollWithTestreglarManualSelection() {
      RestAssured.defaultParser = Parser.JSON
      val body = kontrollInitBody

      /* Create default kontroll */
      val location =
          given()
              .port(port)
              .body(body)
              .contentType("application/json")
              .post("/kontroller")
              .then()
              .statusCode(equalTo(201))
              .extract()
              .header("Location")
      val opprettetKontroll = get(location).`as`(Kontroll::class.java)

      val testregel = get(testregelLocationForId).`as`(Testregel::class.java)

      val updateBody =
          mapOf(
              "kontroll" to opprettetKontroll,
              "testreglar" to
                  mapOf("regelsettId" to null, "testregelIdList" to listOf(testregel.id)),
              "kontrollSteg" to KontrollSteg.Testreglar)

      given()
          .port(port)
          .body(updateBody)
          .contentType("application/json")
          .put(location)
          .then()
          .statusCode(equalTo(204))
      val lagretKontroll = get(location).`as`(Kontroll::class.java)

      assertThat(lagretKontroll.testreglar?.testregelList?.map { it.id })
          .isEqualTo(listOf(testregel.id))
    }

    private fun createDefaultTestregel(): String {
      val localtionForId =
          given()
              .port(port)
              .body(
                  TestregelInit(
                      testregelId = "testregel_skal_slettes",
                      namn = "testregel_skal_slettes",
                      kravId = 1,
                      status = TestregelStatus.publisert,
                      type = TestregelInnholdstype.nett,
                      modus = TestregelModus.manuell,
                      spraak = TestlabLocale.nb,
                      datoSistEndra = Instant.now().minusSeconds(61),
                      testregelSchema = "{\"gaaTil\": 1}",
                      innhaldstypeTesting = 1,
                      tema = 1,
                      testobjekt = 1,
                      kravTilSamsvar = ""))
              .contentType("application/json")
              .post("/v1/testreglar")
              .then()
              .statusCode(equalTo(201))
              .extract()
              .header("Location")

      return "http://localhost:$port$localtionForId"
    }
  }
}
