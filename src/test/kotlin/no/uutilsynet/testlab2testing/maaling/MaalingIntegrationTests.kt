package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.net.URL
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
            "/v1/maalinger", mapOf("navn" to "testmåling", "url" to "https://www.uutilsynet.no"))
    assertThat(location.toString(), matchesPattern(locationPattern))
  }

  @Nested
  @DisplayName("gitt at det finnes en måling i databasen")
  inner class DatabaseHasAtLeastOneMaaling(@Autowired val restTemplate: TestRestTemplate) {
    private var location: URI
    private val url = URL("https://www.example.com")

    init {
      location =
          restTemplate.postForLocation(
              "/v1/maalinger", mapOf("navn" to "example", "url" to url.toString()))
    }

    @Test
    @DisplayName("så skal vi klare å hente den ut")
    fun getMaaling() {
      val (id, navn, urlFromApi) = restTemplate.getForObject(location, GetMaalingDTO::class.java)

      assertThat(id, instanceOf(Int::class.java))
      assertThat(navn, equalTo("example"))
      assertThat(urlFromApi, equalTo(url.toString()))
    }

    @Test
    @DisplayName("så skal vi kunne finne den i lista over alle målinger")
    fun listMaalinger() {
      val (id) = restTemplate.getForObject(location, GetMaalingDTO::class.java)
      val maalingList = object : ParameterizedTypeReference<List<GetMaalingDTO>>() {}

      val maalinger: ResponseEntity<List<GetMaalingDTO>> =
          restTemplate.exchange("/v1/maalinger", HttpMethod.GET, HttpEntity.EMPTY, maalingList)!!
      val thisMaaling = maalinger.body?.find { it.id == id }!!

      assertThat(thisMaaling.id, equalTo(id))
      assertThat(thisMaaling.navn, equalTo("example"))
      assertThat(thisMaaling.url, equalTo(url.toString()))
    }

    @Test
    @DisplayName("så skal den ha en status")
    fun shouldHaveStatus() {
      val responseData = restTemplate.getForObject(location, String::class.java)
      val maaling = JSONObject(responseData)

      assertThat(maaling["status"], equalTo("planlegging"))
    }

    @Test
    @DisplayName("alle målinger skal ha en status")
    fun allShouldHaveStatus() {
      val response = restTemplate.getForObject("/v1/maalinger", String::class.java)
      val jsonArray = JSONArray(response)
      for (i in 0 until jsonArray.length()) {
        val item = jsonArray.getJSONObject(i)
        assertThat(item["status"], equalTo("planlegging"))
      }
    }

    @Test
    @DisplayName("så skal den ha en liste med overganger til gyldige tilstander")
    fun listTransitions() {
      val maaling = restTemplate.getForObject(location, GetMaalingDTO::class.java)
      assertThat(maaling.aksjoner.size, greaterThan(0))
    }

    @Test
    @DisplayName(
        "når målingen har status 'planlegging', så skal det være en aksjon for å gå til 'crawling'")
    fun actionFromPlanlegging() {
      val maaling = restTemplate.getForObject(location, GetMaalingDTO::class.java)
      assert(maaling.status == "planlegging")
      val expectedData = mapOf("status" to "crawling")

      val aksjon = maaling.aksjoner.first()
      assertThat(aksjon, instanceOf(Aksjon.StartCrawling::class.java))
      assertThat(aksjon.metode, equalTo(Metode.PATCH))
      assertThat((aksjon as Aksjon.StartCrawling).href, equalTo(location))
      assertThat(aksjon.data, equalTo(expectedData))
    }
  }

  @Test
  @DisplayName("en måling som ikke finnes i databasen skal returnere 404")
  fun getNonExisting() {
    val entity =
        restTemplate.exchange(
            "/v1/maalinger/0", HttpMethod.GET, HttpEntity.EMPTY, Maaling.Planlegging::class.java)
    assertThat(entity.statusCode, equalTo(HttpStatus.NOT_FOUND))
  }
}
