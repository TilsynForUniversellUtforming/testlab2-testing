package no.uutilsynet.testlab2testing.kontroll

import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import io.restassured.path.json.JsonPath
import io.restassured.path.json.JsonPath.from
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@DisplayName("KontrollResource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KontrollResourceTest {
  @LocalServerPort var port: Int = 0

  @Test
  @DisplayName("når vi oppretter en kontroll så skal vi få en URI som resultat i location")
  fun createKontroll() {
    val body =
        mapOf(
            "tittel" to "testkontroll",
            "saksbehandler" to "Ola Nordmann",
            "sakstype" to "Forvaltningssak",
            "arkivreferanse" to "1234")
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
    val body =
        mapOf(
            "tittel" to "testkontroll",
            "saksbehandler" to "Ola Nordmann",
            "sakstype" to "Forvaltningssak",
            "arkivreferanse" to "1234")
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
    val body =
        mapOf(
            "tittel" to "testkontroll",
            "saksbehandler" to "Ola Nordmann",
            "sakstype" to "Forvaltningssak",
            "arkivreferanse" to "1234")
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
    assertThat(json.get<String>("sakstype")).isEqualTo("Forvaltningssak")
    assertThat(json.get<String>("arkivreferanse")).isEqualTo("1234")
  }
}
