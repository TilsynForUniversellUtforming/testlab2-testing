package no.uutilsynet.testlab2testing.kontroll

import io.restassured.RestAssured.given
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
    given()
        .port(port)
        .body(
            """
          {
            "tittel": "testkontroll",
            "saksbehandler": "Ola Nordmann",
            "sakstype": "Forvaltningssak",
            "arkivreferanse": "1234"
          }
          """)
        .contentType("application/json")
        .post("/kontroller")
        .then()
        .statusCode(equalTo(201))
        .header("Location", startsWith("http://localhost:$port/kontroller/"))
  }
}
