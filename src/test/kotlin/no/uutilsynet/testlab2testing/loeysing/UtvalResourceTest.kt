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
  @DisplayName(
      "når vi oppretter eit utval med navn og ei liste med løysingar, så skal vi få 201 Created, og uri til utvalet i Location-headeren")
  fun oprettUtvalMedLoeysingar() {
    val loeysingar = listOf(1, 2)
    val response = utvalResource.createUtval(NyttUtval(namn, loeysingar))
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
    assertThat(response.headers.location?.toString()).isNotNull()
  }

  @Test
  @DisplayName("når vi har opprettet eit utval, så skal vi kunne hente det ut")
  fun henteUtval() {
    val loeysingar = listOf(1, 2)
    val response = utvalResource.createUtval(NyttUtval(namn, loeysingar))
    val utval = restTemplate.getForObject(response.headers.location!!, Utval::class.java)
    assertThat(utval.namn).isEqualTo(namn)
    assertThat(utval.loeysingar.map { it.id }).isEqualTo(loeysingar)
  }

  @Test
  @DisplayName("vi skal kunne hente alle utval")
  fun henteAlleUtval() {
    val utval = utvalResource.getUtvalList().body!!
    assertThat(utval).isInstanceOf(List::class.java)
  }

  @Test
  @DisplayName(
      "når vi oppretter eit utval med løysingar som ikkje finst, så skal vi få 400 Bad Request")
  fun opprettUtvalMedLoeysingarSomIkkjeFinst() {
    val alleLoeysingar = loeysingResourceV2.getLoeysingList().body!!
    val loeysingarSomIkkjeFinst =
        if (alleLoeysingar.isEmpty()) listOf(1) else listOf(alleLoeysingar.maxOf { it.id } + 1)
    val response = utvalResource.createUtval(NyttUtval(namn, loeysingarSomIkkjeFinst))
    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  @DisplayName("vi skal kunne slette eit utval")
  fun slettUtval() {
    val loeysingar = listOf(1, 2)
    val response = utvalResource.createUtval(NyttUtval(namn, loeysingar))
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

    val utval: Utval? = restTemplate.getForObject(response.headers.location!!, Utval::class.java)
    assertThat(utval?.id).isNotNull()

    utvalResource.deleteUtval(utval?.id!!)
    val sletta: Utval? = restTemplate.getForObject(response.headers.location!!, Utval::class.java)
    assertThat(sletta).isNull()
  }
}
