package no.uutilsynet.testlab2testing.styringsdata

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import kotlin.properties.Delegates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class StyringsdataResourceTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val styringsdataDAO: StyringsdataDAO
) {
  private var styringsdataId: Int by Delegates.notNull()
  private lateinit var location: URI

  @BeforeAll
  fun setUp() {
    styringsdataId = createStyringsdata()
  }

  @Test
  @Order(1)
  @DisplayName("Skal kunne opprette et nytt styringsdata objekt")
  fun testCreateStyringsdata() {
    val styringsdata =
        Styringsdata(
            id = null,
            loeysingId = 1,
            kontrollId = 1,
            ansvarleg = "Test Ansvarleg",
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(50),
            reaksjon = Reaksjonstype.reaksjon,
            paaleggReaksjon = Reaksjonstype.reaksjon,
            paaleggKlageReaksjon = Reaksjonstype.reaksjon,
            botReaksjon = Reaksjonstype.reaksjon,
            botKlageReaksjon = Reaksjonstype.reaksjon,
            paalegg = null,
            paaleggKlage = null,
            bot = null,
            botKlage = null,
            sistLagra = Instant.now())

    val responseEntity = restTemplate.postForEntity("/styringsdata", styringsdata, Unit::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
    location = responseEntity.headers.location!!
  }

  @Test
  @Order(2)
  @DisplayName("Skal kunne hente et eksisterende styringsdata objekt")
  fun getStyringsdata() {
    val responseEntity = restTemplate.getForEntity(location, Styringsdata::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.ansvarleg).isEqualTo("Test Ansvarleg")
  }

  @Test
  @Order(3)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med paalegg")
  fun updateStyringsdataWithPaalegg() {
    val original = restTemplate.getForObject(location, Styringsdata::class.java)

    val updated =
        original.copy(
            paalegg =
                Paalegg(
                    id = null,
                    vedtakDato = LocalDate.now().minusDays(10),
                    frist = LocalDate.now().plusDays(20)))

    restTemplate.put(location, updated)

    val responseEntity = restTemplate.getForEntity(location, Styringsdata::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.paalegg).isNotNull
    assertThat(body.paalegg!!.vedtakDato).isEqualTo(LocalDate.now().minusDays(10))
  }

  @Test
  @Order(4)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med paaleggKlage")
  fun updateStyringsdataWithPaaleggKlage() {
    val original = restTemplate.getForObject(location, Styringsdata::class.java)

    val updated =
        original.copy(
            paaleggKlage =
                Klage(
                    id = null,
                    klageMottattDato = LocalDate.now().minusDays(5),
                    klageAvgjortDato = LocalDate.now().plusDays(15),
                    resultatKlageTilsyn = ResultatKlage.stadfesta,
                    klageDatoDepartement = LocalDate.now().plusDays(20),
                    resultatKlageDepartement = ResultatKlage.stadfesta))

    restTemplate.put(location, updated)

    val responseEntity = restTemplate.getForEntity(location, Styringsdata::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.paaleggKlage).isNotNull
    assertThat(body.paaleggKlage!!.klageMottattDato).isEqualTo(LocalDate.now().minusDays(5))
  }

  @Test
  @Order(5)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med bot")
  fun updateStyringsdataWithBot() {
    val original = restTemplate.getForObject(location, Styringsdata::class.java)

    val updated =
        original.copy(
            bot =
                Bot(
                    id = null,
                    beloepDag = 100,
                    oekingEtterDager = 10,
                    oekningType = BotOekningType.kroner,
                    oekingSats = 10,
                    vedtakDato = LocalDate.now().plusDays(30),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusDays(60),
                    kommentar = "Test kommentar"))

    restTemplate.put(location, updated)

    val responseEntity = restTemplate.getForEntity(location, Styringsdata::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.bot).isNotNull
    assertThat(body.bot!!.beloepDag).isEqualTo(100)
  }

  @Test
  @Order(6)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med botKlage")
  fun updateStyringsdataWithBotKlage() {
    val original = restTemplate.getForObject(location, Styringsdata::class.java)

    val updated =
        original.copy(
            botKlage =
                Klage(
                    id = null,
                    klageMottattDato = LocalDate.now().minusDays(5),
                    klageAvgjortDato = LocalDate.now().plusDays(15),
                    resultatKlageTilsyn = ResultatKlage.stadfesta,
                    klageDatoDepartement = LocalDate.now().plusDays(20),
                    resultatKlageDepartement = ResultatKlage.stadfesta))

    restTemplate.put(location, updated)

    val responseEntity = restTemplate.getForEntity(location, Styringsdata::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.botKlage).isNotNull
    assertThat(body.botKlage!!.klageMottattDato).isEqualTo(LocalDate.now().minusDays(5))
  }

  private fun createStyringsdata(): Int {
    val styringsdata =
        Styringsdata(
            id = null,
            loeysingId = 1,
            kontrollId = 1,
            ansvarleg = "Test Ansvarleg",
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(50),
            reaksjon = Reaksjonstype.reaksjon,
            paaleggReaksjon = Reaksjonstype.reaksjon,
            paaleggKlageReaksjon = Reaksjonstype.reaksjon,
            botReaksjon = Reaksjonstype.reaksjon,
            botKlageReaksjon = Reaksjonstype.reaksjon,
            paalegg = null,
            paaleggKlage = null,
            bot = null,
            botKlage = null,
            sistLagra = Instant.now())
    return styringsdataDAO.createStyringsdata(styringsdata).getOrThrow()
  }
}
