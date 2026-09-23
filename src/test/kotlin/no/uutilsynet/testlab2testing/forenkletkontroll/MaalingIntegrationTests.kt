package no.uutilsynet.testlab2testing.forenkletkontroll

import jakarta.validation.ClockProvider
import java.net.URI
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import no.uutilsynet.testlab2testing.common.TestUtils
import no.uutilsynet.testlab2testing.common.WebConfig
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingRequestBody
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingTestName
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.testRegelList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.uutilsynetLoeysing
import no.uutilsynet.testlab2testing.forenkletkontroll.dto.EditMaalingDTO
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.utval.UtvalDAO
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.oneOf
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody
import org.springframework.test.web.servlet.client.returnResult
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringJUnitConfig(WebConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class MaalingIntegrationTests(
    val maalingDAO: MaalingDAO,
    val utvalDAO: UtvalDAO,
    val testUtils: TestUtils
) {
  @MockitoBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient
  @MockitoBean lateinit var testregelClient: TestregelClient
  @MockitoBean lateinit var clockProvider: ClockProvider

  lateinit var client: RestTestClient

  val utvalTestName = "testutval"
  val loeysingsIdList = loeysingList.map { it.id }
  val singleLoeysing = listOf(loeysingList[0])
  val testregel = testUtils.createTestregel()

  @BeforeEach
  fun beforeEach(context: WebApplicationContext) {
    client = RestTestClient.bindToApplicationContext(context).build()
    doReturn(loeysingList).`when`(loeysingsRegisterClient).getMany(loeysingList.map { it.id })
    doReturn(loeysingList)
        .`when`(loeysingsRegisterClient)
        .getMany(loeysingList.map { it.id }, maalingDateStart)
    doReturn(singleLoeysing)
        .`when`(loeysingsRegisterClient)
        .getMany(singleLoeysing.map { it.id }, maalingDateStart)
    doReturn(singleLoeysing).`when`(loeysingsRegisterClient).getMany(singleLoeysing.map { it.id })
    doReturn(Clock.fixed(maalingDateStart, ZoneId.systemDefault())).`when`(clockProvider).clock
    doReturn(Result.success(listOf(testregel)))
        .`when`(testregelClient)
        .getTestregelListFromIds(listOf(testregel.id))
    doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelList()
  }

  @AfterAll
  fun cleanup() {
    maalingDAO.jdbcTemplate.update(
        "delete from maalingv1 where navn = :navn", mapOf("navn" to maalingTestName))
    maalingDAO.jdbcTemplate.update(
        "delete from utval where namn = :namn", mapOf("namn" to utvalTestName))
  }

  @Test
  @DisplayName("vi kan opprette en ny måling basert på ei liste med løsninger")
  fun postNewMaaling() {
    val locationPattern = """/v1/maalinger/\d+"""
    val location =
        client
            .post()
            .uri("/v1/maalinger")
            .body(maalingRequestBody)
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .exists("Location")
            .returnResult()
            .responseHeaders
            .location

    assertThat(location?.toString(), matchesPattern(locationPattern))
  }

  @Test
  @DisplayName("vi kan opprette en ny måling basert på et utvalg")
  fun postNewMaalingWithUtvalg() {
    val utvalId = utvalDAO.createUtval(utvalTestName, loeysingList.map { it.id }).getOrThrow()
    val requestBody =
        mapOf(
            "navn" to maalingTestName,
            "datoStart" to maalingDateStart,
            "utvalId" to utvalId,
            "testregelIdList" to testRegelList.map { it.id },
            "crawlParameters" to mapOf("maxLenker" to 10, "talLenker" to 10))
    val location =
        client
            .post()
            .uri("/v1/maalinger")
            .body(requestBody)
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .exists("Location")
            .returnResult()
            .responseHeaders
            .location

    val locationPattern = """/v1/maalinger/\d+"""
    Assertions.assertThat(location).isNotNull
    assertThat(location?.toString(), matchesPattern(locationPattern))
  }

  @Test
  @DisplayName(
      "når vi oppretter en ny måling, men mangler utvalg og løsninger, så får vi en feilmelding")
  fun postNewMaalingWithoutUtvalgAndLoeysing() {
    val requestBody =
        mapOf(
            "navn" to maalingTestName,
            "testregelIdList" to testRegelList.map { it.id },
            "crawlParameters" to mapOf("maxLenker" to 10, "talLenker" to 10))
    client
        .post()
        .uri("/v1/maalinger")
        .body(requestBody)
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest
  }

  @Test
  @DisplayName(
      "når vi har opprettet en måling basert på et utvalg, så skal utvalgs-ID lagres på målingen")
  fun saveUtvalId() {
    doReturn(loeysingList)
        .`when`(loeysingsRegisterClient)
        .getMany(loeysingList.map { it.id }, maalingDateStart)
    val utvalId = utvalDAO.createUtval(utvalTestName, loeysingsIdList)
    val requestBody =
        mapOf(
            "navn" to maalingTestName,
            "datoStart" to maalingDateStart,
            "utvalId" to utvalId,
            "testregelIdList" to testRegelList.map { it.id },
            "crawlParameters" to mapOf("maxLenker" to 10, "talLenker" to 10))
    val location =
        client
            .post()
            .uri("/v1/maalinger")
            .body(requestBody)
            .header("Content-Type", "application/json")
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .exists("Location")
            .returnResult()
            .responseHeaders
            .location

    Assertions.assertThat(location).isNotNull

    val maalingId = location!!.path.split("/").last().toInt()
    val utvalIdFromDatabase =
        maalingDAO.jdbcTemplate
            .query("select utval_id from maalingv1 where id = :id", mapOf("id" to maalingId)) {
                rs,
                _,
              ->
              rs.getInt("utval_id")
            }
            .first()

    assertThat(utvalIdFromDatabase, equalTo(utvalId.getOrThrow()))
  }

  @Test
  @DisplayName("det er ikke mulig å opprette en ny måling hvis løsningen ikke finnes i databasen")
  fun postInvalidNewMaaling() {
    val requestBody = mapOf("navn" to maalingTestName, "loeysingIdList" to listOf(1, 2, 3, 11))
    val result =
        client
            .post()
            .uri("/v1/maalinger")
            .body(requestBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<String>()

    assertThat(result.status, equalTo(HttpStatus.BAD_REQUEST))
  }

  @Nested
  @DisplayName("gitt at det finnes en måling i databasen")
  inner class DatabaseHasAtLeastOneMaaling {
    private var location: URI = getLocation()

    private fun getLocation(): URI {
      doReturn(loeysingList).`when`(loeysingsRegisterClient).getMany(loeysingList.map { it.id })
      doReturn(loeysingList)
          .`when`(loeysingsRegisterClient)
          .getMany(loeysingList.map { it.id }, maalingDateStart)
      Mockito.`when`(clockProvider.clock)
          .thenReturn(Clock.fixed(maalingDateStart, ZoneId.systemDefault()))
      doReturn(Result.success(listOf(testregel)))
          .`when`(testregelClient)
          .getTestregelListFromIds(listOf(testregel.id))
      doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelList()
      val result =
          client
              .post()
              .uri("/v1/maalinger")
              .body(maalingRequestBody)
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult<Void>()
      return result.responseHeaders.location ?: error("No location header")
    }

    @Test
    @DisplayName("så skal vi klare å hente den ut")
    fun getMaaling() {

      val response =
          client.get().uri(location).exchange().expectStatus().isOk.expectBody<MaalingDTO>()

      val (id, navn, loeysingListFromApi) =
          response.returnResult().responseBody ?: error("No response body")

      assertThat(id, instanceOf(Int::class.java))
      assertThat(navn, equalTo(maalingTestName))
      Assertions.assertThat(loeysingListFromApi?.map { it.id })
          .containsExactlyInAnyOrderElementsOf(loeysingList.map { it.id })
    }

    @Test
    @DisplayName("så skal vi kunne finne den i lista over alle målinger")
    fun listMaalinger() {
      val response =
          client.get().uri(location).exchange().expectStatus().isOk.expectBody<MaalingDTO>()

      val (id) = response.returnResult().responseBody ?: error("No response body")

      val maalingListResult =
          client
              .get()
              .uri("/v1/maalinger")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(object : ParameterizedTypeReference<List<MaalingListElement>>() {})

      val maalinger = maalingListResult.responseBody ?: error("No response body")
      val thisMaaling = maalinger.find { it.id == id }!!

      assertThat(thisMaaling.id, equalTo(id))
      assertThat(thisMaaling.navn, equalTo(maalingTestName))
    }

    @Test
    @DisplayName("så skal den ha en status")
    fun shouldHaveStatus() {
      val response =
          client.get().uri(location).exchange().expectStatus().isOk.returnResult<String>()

      val responseData = response.responseBody ?: error("No response body")
      val maaling = JSONObject(responseData)

      assertThat(maaling["status"], equalTo("planlegging"))
    }

    @Test
    @DisplayName("alle målinger skal ha en status")
    fun allShouldHaveStatus() {
      val response =
          client.get().uri("/v1/maalinger").exchange().expectStatus().isOk.returnResult<String>()

      val responseData = response.responseBody ?: error("No response body")
      val jsonArray = JSONArray(responseData)
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
      val response =
          client.get().uri(location).exchange().expectStatus().isOk.expectBody<MaalingDTO>()

      val maaling = response.returnResult().responseBody ?: error("No response body")
      assertThat(maaling.aksjoner.size, greaterThan(0))
    }

    @Test
    @DisplayName(
        "når målingen har status 'planlegging', så skal det være en aksjon for å gå til 'crawling'")
    fun actionFromPlanlegging() {
      val response =
          client.get().uri(location).exchange().expectStatus().isOk.expectBody<MaalingDTO>()

      val maaling = response.returnResult().responseBody ?: error("No response body")
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
    val result =
        client
            .get()
            .uri("/v1/maalinger/0")
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult<MaalingDTO>()

    assertThat(result.status, equalTo(HttpStatus.NOT_FOUND))
  }

  @Test
  @DisplayName("Skal kunne endre måling")
  fun updateMaaling() {
    val maaling =
        maalingDAO
            .createMaaling(
                "TestMåling",
                maalingDateStart,
                loeysingsIdList,
                testRegelList.map { it.id },
                CrawlParameters())
            .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }

    val updatedLoeysingList = listOf(maaling.loeysingList[0])
    doReturn(updatedLoeysingList)
        .`when`(loeysingsRegisterClient)
        .getMany(updatedLoeysingList.map { it.id })
    client
        .put()
        .uri("/v1/maalinger")
        .body(
            EditMaalingDTO(
                id = maaling.id,
                navn = maalingTestName,
                loeysingIdList = updatedLoeysingList.map { it.id },
                testregelIdList = testRegelList.map { it.id },
                crawlParameters = null))
        .exchange()
        .expectStatus()
        .isOk

    val updatedMaalingResponse =
        client
            .get()
            .uri("/v1/maalinger/${maaling.id}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(MaalingDTO::class.java)

    val updatedMaaling = updatedMaalingResponse.returnResult().responseBody

    Assertions.assertThat(updatedMaaling).isNotNull
    Assertions.assertThat(updatedMaaling).isInstanceOf(MaalingDTO::class.java)

    val response: MaalingDTO = updatedMaaling!!

    Assertions.assertThat(response.navn).isEqualTo(maalingTestName)
    Assertions.assertThat(response.loeysingList).containsExactlyElementsOf(updatedLoeysingList)
  }

  @Test
  @DisplayName("Skal kunne slette måling")
  fun deleteMaaling() {
    val maaling =
        maalingDAO
            .createMaaling(
                "TestMåling",
                maalingDateStart,
                loeysingList.map { it.id },
                testRegelList.map { it.id },
                CrawlParameters())
            .let { maalingDAO.getMaaling(it) as Maaling.Planlegging }

    val existingMaalingResponse =
        client
            .get()
            .uri("/v1/maalinger/${maaling.id}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<MaalingDTO>()

    val existingMaaling = existingMaalingResponse.returnResult().responseBody

    Assertions.assertThat(existingMaaling).isNotNull
    Assertions.assertThat(existingMaaling).isInstanceOf(MaalingDTO::class.java)

    client.delete().uri("/v1/maalinger/${maaling.id}").exchange().expectStatus().isOk

    val nonExistingMaalingResponse =
        client
            .get()
            .uri("/v1/maalinger/${maaling.id}")
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult<MaalingDTO>()

    Assertions.assertThat(nonExistingMaalingResponse.responseBody).isNull()
  }

  @Nested
  @DisplayName("gitt at det finnes en måling som har status 'kvalitetssikring'")
  inner class StatusKvalitetssikring {

    @Test
    @DisplayName("så har alle crawlresultatene et tidspunkt det ble oppdatert på")
    fun hasTidspunkt() {
      val (key, sistOppdatert) = createMaaling()

      val response =
          client
              .get()
              .uri("/v1/maalinger/$key")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody<MaalingDTO>()

      val maalingFraApi = response.returnResult().responseBody ?: error("No response body")

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

      val response =
          client
              .get()
              .uri("/v1/maalinger/$key")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody<MaalingDTO>()

      val actual = response.returnResult().responseBody ?: error("No response body")

      Assertions.assertThat(actual.aksjoner).anyMatch { aksjon ->
        aksjon.data["status"] == "testing"
      }
    }

    @DisplayName("så får man hentet nettsidene som er crawlet")
    @Test
    fun hasHasCorrectNumberOfNettsider() {

      val (key, _) = createMaaling()

      val response =
          client
              .get()
              .uri("/v1/maalinger/$key/crawlresultat/nettsider?loeysingId=${uutilsynetLoeysing.id}")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(object : ParameterizedTypeReference<List<URL>>() {})

      Assertions.assertThat(response.responseBody!!).containsExactly(uutilsynetLoeysing.url)
    }

    @DisplayName("så får man hentet nettsidene som er crawlet for gitt løysing")
    @Test
    fun hasHasCorrectNumberOfNettsiderWithLoeysing() {
      val (key, _) = createMaaling()

      val response =
          client
              .get()
              .uri("/v1/maalinger/$key/crawlresultat/nettsider?loeysingId=${uutilsynetLoeysing.id}")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(object : ParameterizedTypeReference<List<URL>>() {})

      Assertions.assertThat(response.responseBody!!).containsExactly(uutilsynetLoeysing.url)
    }

    private fun createMaaling(): Pair<Int, Instant> {
      doReturn(singleLoeysing)
          .`when`(loeysingsRegisterClient)
          .getMany(singleLoeysing.map { it.id }, maalingDateStart)
      val crawlParameters = CrawlParameters()
      val id =
          maalingDAO.createMaaling(
              maalingTestName,
              maalingDateStart,
              singleLoeysing.map { it.id },
              testRegelList.map { it.id },
              crawlParameters)
      val planlagtMaaling = maalingDAO.getMaaling(id) as Maaling.Planlegging
      val sistOppdatert = Instant.now()
      val crawlingMaaling =
          Maaling.toCrawling(
              planlagtMaaling,
              listOf(
                  CrawlResultat.Ferdig(
                      1,
                      URI("https://status.uri").toURL(),
                      uutilsynetLoeysing,
                      sistOppdatert,
                      listOf(uutilsynetLoeysing.url))))
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
    val crawlResultat: List<CrawlResultatFerdigDTO>?, // hvis status er 'crawling'
    val status: String,
    val aksjoner: List<Aksjon>,
)

data class CrawlResultatFerdigDTO(
    val loeysing: Loeysing,
    val sistOppdatert: Instant,
    val antallNettsider: Int,
    val statusUrl: URL,
)
