package no.uutilsynet.testlab2testing.kontroll

import io.restassured.RestAssured
import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import io.restassured.parsing.Parser
import io.restassured.path.json.JsonPath
import io.restassured.path.json.JsonPath.from
import jakarta.validation.ClockProvider
import no.uutilsynet.testlab2testing.common.TestUtils
import java.net.URI
import java.time.Clock
import java.time.ZoneId
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.loeysingList
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.TestStatus
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalResource
import no.uutilsynet.testlab2testing.regelsett.Regelsett
import no.uutilsynet.testlab2testing.regelsett.RegelsettCreate
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean

@DisplayName("KontrollResource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KontrollResourceTest(
    @Autowired private val testUtils: TestUtils,
) {
  @LocalServerPort var port: Int = 0
  @MockitoBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient
  @MockitoBean lateinit var clockProvider: ClockProvider
  @MockitoBean lateinit var testregelClient: TestregelClient
    val testregel = testUtils.createTestregel()


    @BeforeEach
  fun beforeEach() {
    doReturn(Clock.fixed(maalingDateStart, ZoneId.systemDefault())).`when`(clockProvider).clock
    doReturn(listOf(loeysingList[0]))
        .`when`(loeysingsRegisterClient)
        .getMany(listOf(loeysingList[0].id), maalingDateStart)
    doReturn(listOf(loeysingList[0]))
        .`when`(loeysingsRegisterClient)
        .getMany(listOf(loeysingList[0].id))
    doReturn(Result.success(listOf(loeysingList[0])))
        .`when`(loeysingsRegisterClient)
        .search(anyString())
      doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelListFromIds(listOf(testregel.id))

  }

  val kontrollInitBody =
      mapOf(
          "kontrolltype" to "manuell-kontroll",
          "tittel" to "testkontroll",
          "saksbehandler" to "Ola Nordmann",
          "sakstype" to "forvaltningssak",
          "kontrolltype" to "inngaaende-kontroll",
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
        .header(
            "Location", org.hamcrest.CoreMatchers.startsWith("http://localhost:$port/kontroller/"))
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
    (1..3).forEach { _ ->
      given()
          .port(port)
          .body(updateBody)
          .contentType("application/json")
          .put(location)
          .then()
          .statusCode(equalTo(204))
    }
  }

  @Test
  @DisplayName("gitt vi har en kontroll, så skal vi kunne oppdatere testreglar med regelsett")
  fun updateKontrollWithTestreglarRegelsett() {
      doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelList()

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

    /* Create regelsett */
    val nyttRegelsett =
        RegelsettCreate(
            namn = "regelsett_skal_slettes",
            modus = testregel.modus,
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
    val regelsett = get("http://localhost:$port$regelsettLocationForId").`as`(Regelsett::class.java)

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
      doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelList()



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

    val updateBody =
        mapOf(
            "kontroll" to opprettetKontroll,
            "testreglar" to mapOf("regelsettId" to null, "testregelIdList" to listOf(testregel.id)),
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

  @Test
  @DisplayName("vi skal kunne hente ut en liste med alle kontroller")
  fun getKontrollerTest() {
      doReturn(Result.success(listOf(testregel))).`when`(testregelClient).getTestregelList()
      RestAssured.defaultParser = Parser.JSON
    val kontroller =
        given()
            .port(port)
            .accept("application/json")
            .get("/kontroller")
            .then()
            .statusCode(equalTo(200))
            .extract()
            .body()
            .`as`(Array<KontrollResource.KontrollListItem>::class.java)
    assertThat(kontroller).isNotNull()
    kontroller.forEach { kontroll ->
      assertThat(kontroll).isInstanceOf(KontrollResource.KontrollListItem::class.java)
    }
  }

  @Test
  @DisplayName("gitt vi har en kontroll med løsyinger så skal vi kunne legge inn sideutval")
  fun updateKontrollWithSideutval() {
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

    /* Add loesying */
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

    val updateBodyUtval =
        mapOf(
            "kontroll" to opprettetKontroll,
            "utvalId" to utval.id,
            "kontrollSteg" to KontrollSteg.Utval)

    given()
        .port(port)
        .body(updateBodyUtval)
        .contentType("application/json")
        .put(location)
        .then()
        .statusCode(equalTo(204))

    /* Add sideutval */
    val updateBody =
        mapOf(
            "kontroll" to opprettetKontroll,
            "sideutvalList" to
                listOf(
                    mapOf(
                        "loeysingId" to utval.loeysingar.first().id,
                        "typeId" to 1,
                        "begrunnelse" to "Side med elementer",
                        "url" to "https://www.uutilsynet.no",
                        "egendefinertObjekt" to "")),
            "kontrollSteg" to KontrollSteg.Sideutval)

    given()
        .port(port)
        .body(updateBody)
        .contentType("application/json")
        .put(location)
        .then()
        .statusCode(equalTo(204))
    val lagretKontroll = get(location).`as`(Kontroll::class.java)

    with(lagretKontroll.sideutvalList.first()) {
      assertThat(loeysingId).isEqualTo(utval.loeysingar.first().id)
      assertThat(typeId).isEqualTo(1)
      assertThat(begrunnelse).isEqualTo("Side med elementer")
      assertThat(url).isEqualTo(URI("https://www.uutilsynet.no"))
      assertThat(egendefinertType).isNull()
    }
  }

  @Test
  @DisplayName("en kontroll som ikke er startet skal gi riktig test-status")
  fun getKontrollStatus() {
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
    val id = opprettetKontroll.id

    val statusUrl = location.replace(Regex("/$id$"), "/test-status/$id")
    val status = get(statusUrl).`as`(TestStatus::class.java)
    assertThat(status).isEqualTo(TestStatus.Pending)
  }
}
