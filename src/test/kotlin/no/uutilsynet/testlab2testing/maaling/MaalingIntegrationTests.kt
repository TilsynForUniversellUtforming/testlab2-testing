package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MaalingIntegrationTests(@Autowired val restTemplate: TestRestTemplate) {

  @Test
  @DisplayName("det er mulig å opprette nye målinger")
  fun postNewMaaling() {
    val locationPattern = """/v1/maalinger/\d+"""
    val location =
        restTemplate.postForLocation(
            "/v1/maalinger", mapOf("navn" to "tes≤tmåling", "url" to "https://www.uutilsynet.no"))
    assertThat(location.toString(), matchesPattern(locationPattern))
  }

  @Test
  @DisplayName("en måling som finnes i databasen skal vi klare å finne")
  fun getMaaling() {
    val url = URL("https://www.digdir.no")
    val location =
        restTemplate.postForLocation(
            "/v1/maalinger", mapOf("navn" to "Digdir", "url" to url.toString()))

    val (id, navn, urlFromApi) = restTemplate.getForObject(location, MaalingV1::class.java)

    assertThat(id, instanceOf(Int::class.java))
    assertThat(navn, equalTo("Digdir"))
    assertThat(urlFromApi, equalTo(url))
  }

  @Test
  @DisplayName("en måling som finnes i databasen skal kunne finnes med utlisting")
  fun listMaalinger() {
    val url = URL("https://www.example.com")
    val location =
        restTemplate.postForLocation(
            "/v1/maalinger", mapOf("navn" to "example", "url" to url.toString()))
    val (id) = restTemplate.getForObject(location, MaalingV1::class.java)
    val maalingList = object : ParameterizedTypeReference<List<MaalingV1>>() {}

    val maalinger: ResponseEntity<List<MaalingV1>> =
        restTemplate.exchange("/v1/maalinger", HttpMethod.GET, HttpEntity.EMPTY, maalingList)!!
    val thisMaaling = maalinger.body?.find { it.id == id }!!

    assertThat(thisMaaling.id, equalTo(id))
    assertThat(thisMaaling.navn, equalTo("example"))
    assertThat(thisMaaling.url, equalTo(url))
  }

  @Test
  @DisplayName("en måling som ikke finnes i databasen skal returnere 404")
  fun getNonExisting() {
    val entity =
        restTemplate.exchange(
            "/v1/maalinger/0", HttpMethod.GET, HttpEntity.EMPTY, MaalingV1::class.java)
    assertThat(entity.statusCode, equalTo(HttpStatus.NOT_FOUND))
  }
}
