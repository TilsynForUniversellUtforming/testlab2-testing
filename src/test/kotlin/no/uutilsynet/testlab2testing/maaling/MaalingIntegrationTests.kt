package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.maaling.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingRequestBody
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
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
    val location = restTemplate.postForLocation("/v1/maalinger", maalingRequestBody)
    assertThat(location.toString(), matchesPattern(locationPattern))
  }

  @Nested
  @DisplayName("gitt at det finnes en måling i databasen")
  inner class DatabaseHasAtLeastOneMaaling(@Autowired val restTemplate: TestRestTemplate) {
    private var location: URI = restTemplate.postForLocation("/v1/maalinger", maalingRequestBody)

    @Test
    @DisplayName("så skal vi klare å hente den ut")
    fun getMaaling() {
      val (id, navn, loeysingListFromApi) =
          restTemplate.getForObject(location, MaalingDTO::class.java)

      assertThat(id, instanceOf(Int::class.java))
      assertThat(navn, equalTo("example"))
      assertThat(loeysingListFromApi, equalTo(loeysingList))
    }

    @Test
    @DisplayName("så skal vi kunne finne den i lista over alle målinger")
    fun listMaalinger() {
      val (id) = restTemplate.getForObject(location, MaalingDTO::class.java)
      val maalingList = object : ParameterizedTypeReference<List<MaalingDTO>>() {}

      val maalinger: ResponseEntity<List<MaalingDTO>> =
          restTemplate.exchange("/v1/maalinger", HttpMethod.GET, HttpEntity.EMPTY, maalingList)!!
      val thisMaaling = maalinger.body?.find { it.id == id }!!

      assertThat(thisMaaling.id, equalTo(id))
      assertThat(thisMaaling.navn, equalTo("example"))
      assertThat(thisMaaling.loeysingList, equalTo(loeysingList))
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
        assertThat(item["status"], oneOf("planlegging", "crawling"))
      }
    }

    @Test
    @DisplayName("så skal den ha en liste med overganger til gyldige tilstander")
    fun listTransitions() {
      val maaling = restTemplate.getForObject(location, MaalingDTO::class.java)
      assertThat(maaling.aksjoner.size, greaterThan(0))
    }

    @Test
    @DisplayName(
        "når målingen har status 'planlegging', så skal det være en aksjon for å gå til 'crawling'")
    fun actionFromPlanlegging() {
      val maaling = restTemplate.getForObject(location, MaalingDTO::class.java)
      assert(maaling.status == "planlegging")
      val expectedData = mapOf("status" to "crawling")

      val aksjon = maaling.aksjoner.first()
      assertThat(aksjon, instanceOf(Aksjon.StartCrawling::class.java))
      assertThat(aksjon.metode, equalTo(Metode.PUT))
      assertThat((aksjon as Aksjon.StartCrawling).href, equalTo(URI("${location}/status")))
      assertThat(aksjon.data, equalTo(expectedData))
    }
  }

  @Nested
  @DisplayName("gitt en måling med status 'planlegging'")
  inner class Transitions {
    fun createMaaling(): Pair<MaalingDTO, URI> {
      val location = restTemplate.postForLocation("/v1/maalinger", maalingRequestBody)
      val maaling: MaalingDTO = restTemplate.getForObject(location, MaalingDTO::class.java)
      assert(maaling.status == "planlegging")
      return Pair(maaling, location)
    }

    @Test
    @DisplayName("så skal det gå an å starte crawling")
    @Disabled(
        "skrudd av mens jeg finner en bedre måte å teste dette på, som ikke crawler nettsider for hver gang testen kjører")
    fun startCrawling() {
      val (maaling, location) = createMaaling()

      val aksjon = maaling.aksjoner.find { it is Aksjon.StartCrawling } as Aksjon.StartCrawling
      val entity =
          when (aksjon.metode) {
            Metode.PUT ->
                restTemplate.exchange(
                    aksjon.href, HttpMethod.PUT, HttpEntity(aksjon.data), Unit::class.java)
          }

      assertTrue(entity.statusCode.is2xxSuccessful)

      val oppdatertMaaling = restTemplate.getForObject(location, MaalingDTO::class.java)

      assertThat(oppdatertMaaling.status, equalTo("crawling"))
      val crawlResultat = oppdatertMaaling.crawlResultat
      assertThat(crawlResultat, hasSize(2))
      crawlResultat?.forEach { etCrawlResultat ->
        when (etCrawlResultat) {
          is CrawlResultat.IkkeFerdig -> assertThat(etCrawlResultat.statusUrl, notNullValue())
          else -> fail { "crawlresultatet hadde en uventet status" }
        }
      }
    }
  }

  @Test
  @DisplayName("en måling som ikke finnes i databasen skal returnere 404")
  fun getNonExisting() {
    val entity =
        restTemplate.exchange(
            "/v1/maalinger/0", HttpMethod.GET, HttpEntity.EMPTY, MaalingDTO::class.java)
    assertThat(entity.statusCode, equalTo(HttpStatus.NOT_FOUND))
  }

  @Test
  @DisplayName("Skal hente ut listen med løsninger")
  fun fetchLoesyingList() {
    val responseType = object : ParameterizedTypeReference<List<Loeysing>>() {}

    val entity =
        restTemplate.exchange(
            "/v1/maalinger/loeysingar", HttpMethod.GET, HttpEntity.EMPTY, responseType)

    assertThat(entity.body!![0], equalTo(loeysingList[0]))
  }
}

data class MaalingDTO(
    val id: Int,
    val navn: String,
    val loeysingList: List<Loeysing>?, // hvis status er 'planlegging'
    val crawlResultat: List<CrawlResultat>?, // hvis status er 'crawling'
    val status: String,
    val aksjoner: List<Aksjon>
)
