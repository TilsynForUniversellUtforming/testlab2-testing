package no.uutilsynet.testlab2testing.styringsdata

import no.uutilsynet.testlab2.constants.*
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.styringsdata.Styringsdata.Loeysing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import kotlin.properties.Delegates

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class StyringsdataResourceTest(
    @Autowired val styringsdataDAO: StyringsdataDAO,
    @Autowired val kontrollDAO: KontrollDAO,
) {
  private var styringsdataId: Int by Delegates.notNull()
  private var kontrollId: Int by Delegates.notNull()
  private lateinit var locationLoeysing: URI
  private lateinit var locationKontroll: URI

  lateinit var client: RestTestClient

  @BeforeEach
  fun setupBeforeEach(context: WebApplicationContext) {
    client = RestTestClient.bindToApplicationContext(context).build()
  }

  @BeforeAll
  fun setUp() {
    kontrollId = createTestKontroll()
    styringsdataId = createStyringsdataLoeysing()
  }

  @Test
  @Order(1)
  @DisplayName("Skal kunne opprette et nytt styringsdata objekt")
  fun testCreateStyringsdata() {
    val styringsdata =
        Styringsdata.Loeysing(
            id = null,
            loeysingId = 1,
            kontrollId = kontrollId,
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

    locationLoeysing =
        client
            .post()
            .uri("/styringsdata")
            .body(styringsdata)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .exists("Location")
            .returnResult<Void>()
            .responseHeaders
            .location!!
  }

  @Test
  @Order(2)
  @DisplayName("Skal kunne hente et eksisterende styringsdata objekt")
  fun getStyringsdata() {
    val response = getStyringsdataLoeysing()

    assertThat(response.ansvarleg).isEqualTo("Test Ansvarleg")
  }

  @Test
  @Order(3)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med paalegg")
  fun updateStyringsdataWithPaalegg() {
    val original = getStyringsdataLoeysing()

    val updated =
        original.copy(
            paalegg =
                Paalegg(
                    id = null,
                    vedtakDato = LocalDate.now().minusDays(10),
                    frist = LocalDate.now().plusDays(20)))

    client.put().uri(locationLoeysing).body(updated).exchange().expectStatus().isOk

    val responseEntity = getStyringsdataLoeysing()

    assertThat(responseEntity.paalegg).isNotNull
  }

  @Test
  @Order(4)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med paaleggKlage")
  fun updateStyringsdataWithPaaleggKlage() {
    val original = getStyringsdataLoeysing()

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

    client.put().uri(locationLoeysing).body(updated).exchange().expectStatus().isOk

    val body = getStyringsdataLoeysing()

    assertThat(body.paaleggKlage).isNotNull
    assertThat(body.paaleggKlage!!.klageMottattDato).isEqualTo(LocalDate.now().minusDays(5))
  }

  @Test
  @Order(5)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med bot")
  fun updateStyringsdataWithBot() {
    val original = getStyringsdataLoeysing()

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

    client.put().uri(locationLoeysing).body(updated).exchange().expectStatus().isOk

    val body = getStyringsdataLoeysing()

    assertThat(body.bot).isNotNull
    assertThat(body.bot!!.beloepDag).isEqualTo(100)
  }

  @Test
  @Order(6)
  @DisplayName("Skal kunne oppdatere et eksisterende styringsdata objekt med botKlage")
  fun updateStyringsdataWithBotKlage() {
    val original = getStyringsdataLoeysing()

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

    client.put().uri(locationLoeysing).body(updated).exchange().expectStatus().isOk

    val body = getStyringsdataLoeysing()

    assertThat(body.botKlage).isNotNull
    assertThat(body.botKlage!!.klageMottattDato).isEqualTo(LocalDate.now().minusDays(5))
  }

  @Test
  @Order(7)
  @DisplayName("Skal kunne opprette styringdata for kontroll")
  fun createStyringsdataForKontroll() {
    val styringsdata =
        Styringsdata.Kontroll(
            id = null,
            kontrollId = kontrollId,
            ansvarleg = "Test Ansvarleg Kontroll",
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(50),
            endeligRapportDato = null,
            foerebelsRapportSendtDato = null,
            kontrollAvsluttaDato = null,
            rapportPublisertDato = null,
            status = null,
            svarFoerebelsRapportDato = null,
            varselSendtDato = null,
            sistLagra = Instant.now())

    val response =
        client.post().uri("/styringsdata").body(styringsdata).exchange().expectStatus().isCreated
    locationKontroll = response.returnResult<Void>().responseHeaders.location!!
  }

  @Test
  @Order(8)
  @DisplayName("Skal kunne hente styringdata for kontroll")
  fun getStyringsdataForKontroll() {
    val body = getStyringsdataKontroll()

    assertThat(body.ansvarleg).isEqualTo("Test Ansvarleg Kontroll")
  }

  @Test
  @Order(9)
  @DisplayName("Skal kunne oppdatere styringdata for kontroll")
  fun updateStyringsdataForKontroll() {
    val original = getStyringsdataKontroll()

    val updated =
        original.copy(
            oppretta = LocalDate.now(),
            frist = LocalDate.now().plusDays(1),
            endeligRapportDato = LocalDate.now().plusDays(2),
            foerebelsRapportSendtDato = LocalDate.now().plusDays(3),
            kontrollAvsluttaDato = LocalDate.now().plusDays(4),
            rapportPublisertDato = LocalDate.now().plusDays(5),
            svarFoerebelsRapportDato = LocalDate.now().plusDays(6),
            varselSendtDato = LocalDate.now().plusDays(7),
            status = StyringsdataKontrollStatus.paagar,
        )

    client.put().uri(locationKontroll).body(updated).exchange().expectStatus().isOk

    val body = getStyringsdataKontroll()
    assertThat(body.status).isEqualTo(StyringsdataKontrollStatus.paagar)
    assertThat(body.frist).isEqualTo(LocalDate.now().plusDays(1))
    assertThat(body.endeligRapportDato).isEqualTo(LocalDate.now().plusDays(2))
    assertThat(body.foerebelsRapportSendtDato).isEqualTo(LocalDate.now().plusDays(3))
    assertThat(body.kontrollAvsluttaDato).isEqualTo(LocalDate.now().plusDays(4))
    assertThat(body.rapportPublisertDato).isEqualTo(LocalDate.now().plusDays(5))
    assertThat(body.svarFoerebelsRapportDato).isEqualTo(LocalDate.now().plusDays(6))
    assertThat(body.varselSendtDato).isEqualTo(LocalDate.now().plusDays(7))
  }

  @Test
  @Order(10)
  @DisplayName("Skal finne styringsdata for kontroll")
  fun findStyringsdataForKontroll() {

    val result =
        client
            .get()
            .uri("/styringsdata?kontrollId=$kontrollId")
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<StyringsdataResult>()
            .responseBody!!

    val styringsdataKontrollId = locationKontroll.path.split("/").lastOrNull()
    val styringsdataLoeysingId = locationLoeysing.path.split("/").lastOrNull()

    assertThat(result.styringsdataKontrollId).isEqualTo(Integer.valueOf(styringsdataKontrollId))
    assertThat(result.styringsdataLoeysing.lastOrNull()?.id)
        .isEqualTo(Integer.valueOf(styringsdataLoeysingId))
  }

  private fun createStyringsdataLoeysing(): Int {
    val styringsdata =
        Styringsdata.Loeysing(
            id = null,
            loeysingId = 1,
            kontrollId = kontrollId,
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
    return styringsdataDAO.createStyringsdataLoeysing(styringsdata).getOrThrow()
  }

  private fun createTestKontroll(): Int {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Sakstype.Arkivsak,
            "1234",
            Kontrolltype.InngaaendeKontroll)

    return kontrollDAO.createKontroll(opprettKontroll).getOrThrow()
  }

  private fun getStyringsdataLoeysing(): Styringsdata.Loeysing {
    val response =
        client
            .get()
            .uri(locationLoeysing)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<Styringsdata.Loeysing>()
            .responseBody!!
    return response
  }

  private fun getStyringsdataKontroll(): Styringsdata.Kontroll {
    val body =
        client
            .get()
            .uri(locationKontroll)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<Styringsdata.Kontroll>()
            .responseBody!!
    return body
  }
}
