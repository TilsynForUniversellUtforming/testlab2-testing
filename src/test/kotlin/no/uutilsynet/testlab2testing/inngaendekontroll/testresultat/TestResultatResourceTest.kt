package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.SakDAO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.util.UriComponentsBuilder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestResultatResourceTest(
    @Autowired val sakDAO: SakDAO,
    @Autowired val restTemplate: TestRestTemplate
) {
  private var sakId: Int by Delegates.notNull()
  private var location: URI by Delegates.notNull()

  @Test
  @Order(1)
  @DisplayName("vi skal kunne opprette et nytt testresultat")
  fun nyttTestresultat() {
    sakId = sakDAO.save("000000000").getOrThrow()

    sakDAO
        .update(
            Sak(
                sakId,
                "000000000",
                listOf(
                    Sak.Loeysing(
                        1,
                        listOf(
                            Sak.Nettside(
                                1,
                                "Forside",
                                "https://www.uutilsynet.no/",
                                "forside",
                                "forside"))))))
        .getOrThrow()
    val sak = sakDAO.getSak(sakId).getOrThrow()
    val nettside = sak.loeysingar.first().nettsider.first()

    val responseEntity =
        restTemplate.postForEntity(
            "/testresultat",
            mapOf(
                "sakId" to sakId,
                "loeysingId" to 1,
                "testregelId" to 1,
                "nettsideId" to nettside.id),
            Unit::class.java)

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
    location = responseEntity.headers.location!!
  }

  @Test
  @Order(2)
  @DisplayName("vi skal kunne legge til svar på et testresultat vi har opprettet")
  fun leggeTilSvar() {
    val uri = UriComponentsBuilder.fromUri(location).pathSegment("svar").build().toUri()
    val responseEntity =
        restTemplate.postForEntity(uri, mapOf("steg" to "3.2", "svar" to "ja"), Unit::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
  }

  @Test
  @Order(3)
  @DisplayName(
      "vi skal kunne hente ut et testresultat, og den skal inneholde svaret som er lagt inn")
  fun henteUtTestresultat() {
    val responseEntity =
        restTemplate.getForEntity(location, ResultatManuellKontroll.UnderArbeid::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.svar).containsExactlyInAnyOrder(ResultatManuellKontroll.Svar("3.2", "ja"))
  }

  @Test
  @Order(4)
  @DisplayName(
      "vi skal kunne legge til flere svar på et testresultat vi har opprettet, og hente dem ut igjen")
  fun leggeTilFlereSvar() {
    val uri = UriComponentsBuilder.fromUri(location).pathSegment("svar").build().toUri()
    val responseEntity =
        restTemplate.postForEntity(uri, mapOf("steg" to "3.3", "svar" to "nei"), Unit::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val testresultat =
        restTemplate.getForObject(location, ResultatManuellKontroll.UnderArbeid::class.java)

    assertThat(testresultat.svar)
        .containsExactlyInAnyOrder(
            ResultatManuellKontroll.Svar("3.2", "ja"), ResultatManuellKontroll.Svar("3.3", "nei"))
  }

  @Test
  @Order(5)
  @DisplayName("vi skal kunne oppdatere et svar")
  fun oppdatereSvar() {
    val uri = UriComponentsBuilder.fromUri(location).pathSegment("svar").build().toUri()
    val responseEntity =
        restTemplate.postForEntity(uri, mapOf("steg" to "3.2", "svar" to "nei"), Unit::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val testresultat =
        restTemplate.getForObject(location, ResultatManuellKontroll.UnderArbeid::class.java)

    assertThat(testresultat.svar)
        .containsExactlyInAnyOrder(
            ResultatManuellKontroll.Svar("3.2", "nei"), ResultatManuellKontroll.Svar("3.3", "nei"))
  }

  @Test
  @Order(6)
  @DisplayName("vi skal kunne oppdatere testresultatet med elementomtale")
  fun oppdatereTestresultat() {
    val testresultat =
        restTemplate.getForObject(location, ResultatManuellKontroll.UnderArbeid::class.java)
    val endret = testresultat.copy(elementOmtale = "elementomtale")

    restTemplate.put(location, endret)

    val oppdatert =
        restTemplate.getForObject(location, ResultatManuellKontroll.UnderArbeid::class.java)

    assertThat(oppdatert.elementOmtale).isEqualTo("elementomtale")
  }

  @Test
  @Order(7)
  @DisplayName(
      "når vi oppdaterer resultatet med elementresultat og elementutfall, så skal også tidspunktet settes")
  fun oppdatereTestresultatMedElementresultatOgElementutfall() {
    val start = Instant.now()
    val testresultat =
        restTemplate.getForObject(location, ResultatManuellKontroll.UnderArbeid::class.java)
    val endret =
        testresultat.copy(elementResultat = "elementresultat", elementUtfall = "elementutfall")

    restTemplate.put(location, endret)

    val oppdatert = restTemplate.getForObject(location, ResultatManuellKontroll.Ferdig::class.java)

    assertThat(oppdatert.elementResultat).isEqualTo("elementresultat")
    assertThat(oppdatert.elementUtfall).isEqualTo("elementutfall")
    assertThat(oppdatert.testVartUtfoert).isBetween(start, Instant.now())
  }
}
