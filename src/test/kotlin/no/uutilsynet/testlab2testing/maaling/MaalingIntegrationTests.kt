package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.uutilsynet.testlab2testing.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.maaling.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingRequestBody
import no.uutilsynet.testlab2testing.maaling.TestConstants.maalingTestName
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MaalingIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val maalingDAO: MaalingDAO
) {

  @AfterAll
  fun cleanup() {
    maalingDAO.jdbcTemplate.update(
        "delete from maalingv1 where navn = :navn", MapSqlParameterSource("navn", maalingTestName))
  }

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
      assertThat(navn, equalTo(maalingTestName))
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
      assertThat(thisMaaling.navn, equalTo(maalingTestName))
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
        assertThat(
            item["status"],
            oneOf("planlegging", "crawling", "kvalitetssikring", "testing", "testing_ferdig"))
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
      restTemplate.getForObject(location, Map::class.java)
      assert(maaling.status == "planlegging")
      val expectedData = mapOf("status" to "crawling")
      val aksjon = maaling.aksjoner.first()

      assertThat(aksjon, instanceOf(Aksjon.StartCrawling::class.java))
      assertThat(aksjon.metode, equalTo("PUT"))
      assertThat((aksjon as Aksjon.StartCrawling).href, equalTo(URI("${location}/status")))
      assertThat(aksjon.data, equalTo(expectedData))
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

  @Test
  @DisplayName("Skal kunne endre måling")
  fun updateMaaling() {
    val maaling =
        maalingDAO.createMaaling("TestMåling", loeysingList.map { it.id }, CrawlParameters()).let {
          maalingDAO.getMaaling(it) as Maaling.Planlegging
        }

    restTemplate.exchange(
        "/v1/maalinger",
        HttpMethod.PUT,
        HttpEntity(
            EditMaalingDTO(
                id = maaling.id,
                navn = maalingTestName,
                loeysingIdList = listOf(maaling.loeysingList[0].id),
                crawlParameters = null)),
        Unit::class.java)

    val updatedMaaling =
        restTemplate.exchange(
            "/v1/maalinger/${maaling.id}", HttpMethod.GET, HttpEntity.EMPTY, MaalingDTO::class.java)

    Assertions.assertThat(updatedMaaling?.body).isNotNull
    Assertions.assertThat(updatedMaaling?.body).isInstanceOf(MaalingDTO::class.java)

    val response: MaalingDTO = updatedMaaling.body!!

    Assertions.assertThat(response.navn).isEqualTo(maalingTestName)
    Assertions.assertThat(response.loeysingList).containsExactly(uutilsynetLoeysing)
  }

  @Test
  @DisplayName("Skal kunne slette måling")
  fun deleteMaaling() {
    val maaling =
        maalingDAO.createMaaling("TestMåling", loeysingList.map { it.id }, CrawlParameters()).let {
          maalingDAO.getMaaling(it) as Maaling.Planlegging
        }

    val existingMaaling =
        restTemplate.exchange(
            "/v1/maalinger/${maaling.id}", HttpMethod.GET, HttpEntity.EMPTY, MaalingDTO::class.java)

    Assertions.assertThat(existingMaaling?.body).isNotNull
    Assertions.assertThat(existingMaaling?.body).isInstanceOf(MaalingDTO::class.java)

    restTemplate.delete("/v1/maalinger/${maaling.id}")

    val nonExistingMaaling =
        restTemplate.exchange(
            "/v1/maalinger/${maaling.id}", HttpMethod.GET, HttpEntity.EMPTY, MaalingDTO::class.java)

    Assertions.assertThat(nonExistingMaaling?.body).isNull()
  }

  @Nested
  @DisplayName("gitt at det finnes en måling som har status 'kvalitetssikring'")
  inner class StatusKvalitetssikring {
    @Test
    @DisplayName("så har alle crawlresultatene et tidspunkt det ble oppdatert på")
    fun hasTidspunkt() {
      val (key, sistOppdatert) = createMaaling()

      val maalingFraApi = restTemplate.getForObject("/v1/maalinger/$key", MaalingDTO::class.java)

      // Vi mister noe nøyaktighet i noen tilfeller når vi har lagret tidspunktet i databasen og
      // hentet det tilbake. Derfor kutter vi nøyaktigheten til sekunder, som er godt nok her.
      val actual =
          maalingFraApi.crawlResultat?.first()?.sistOppdatert?.truncatedTo(ChronoUnit.SECONDS)
      val expected = sistOppdatert.truncatedTo(ChronoUnit.SECONDS)
      assertThat(actual, equalTo(expected))
    }

    @DisplayName("så har denne målingen en aksjon for å gå til `testing`")
    @Test
    fun hasTestingAction() {
      val (key, _) = createMaaling()

      val actual = restTemplate.getForObject("/v1/maalinger/$key", MaalingDTO::class.java)

      Assertions.assertThat(actual.aksjoner).anyMatch { aksjon ->
        aksjon.data["status"] == "testing"
      }
    }

    private fun createMaaling(): Pair<Int, Instant> {
      val crawlParameters = CrawlParameters()
      val id = maalingDAO.createMaaling(maalingTestName, listOf(1), crawlParameters)
      val planlagtMaaling =
          Maaling.Planlegging(id, maalingTestName, listOf(uutilsynetLoeysing), crawlParameters)
      val sistOppdatert = Instant.now()
      val crawlingMaaling =
          Maaling.toCrawling(
              planlagtMaaling,
              listOf(
                  CrawlResultat.Ferdig(
                      listOf(URL(uutilsynetLoeysing.url, "/")),
                      URL("https://status.uri"),
                      uutilsynetLoeysing,
                      sistOppdatert)))
      val kvalitetssikring = Maaling.toKvalitetssikring(crawlingMaaling)!!
      maalingDAO.save(kvalitetssikring).getOrThrow()
      return Pair(id, sistOppdatert)
    }
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
