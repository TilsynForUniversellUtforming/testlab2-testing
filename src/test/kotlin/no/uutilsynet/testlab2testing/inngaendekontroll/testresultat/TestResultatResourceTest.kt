package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.SakDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll.Svar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus

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
    sakId = sakDAO.save("Testheim kommune", "000000000").getOrThrow()

    sakDAO
        .update(
            Sak(
                sakId,
                "Testheim kommune",
                "000000000",
                loeysingar =
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
                "nettsideId" to nettside.id,
                "brukar" to mapOf("brukarnamn" to "testbrukar@digdir.no", "namn" to "Test Brukar")),
            Unit::class.java)

    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.CREATED)
    location = responseEntity.headers.location!!
  }

  val svar = listOf(Svar("2.2", "ja"), Svar("3.1", "iframe nummer 1"), Svar("3.2", "ja"))

  @Test
  @Order(2)
  @DisplayName("vi skal kunne legge til svar på et testresultat vi har opprettet")
  fun leggeTilSvar() {
    val testresultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endret = testresultat.copy(svar = svar)
    assertDoesNotThrow { restTemplate.put(location, endret) }
  }

  @Test
  @Order(3)
  @DisplayName("vi skal kunne hente ut et testresultat, og den skal inneholde brukaren")
  fun skalInneholdeBrukar() {
    val resultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    assertThat(resultat.brukar).isEqualTo(Brukar("testbrukar@digdir.no", "Test Brukar"))
  }

  @Test
  @Order(3)
  @DisplayName(
      "vi skal kunne hente ut et testresultat, og den skal inneholde svaret som er lagt inn")
  fun henteUtTestresultat() {
    val responseEntity = restTemplate.getForEntity(location, ResultatManuellKontroll::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)

    val body = responseEntity.body!!
    assertThat(body.svar).containsExactlyElementsOf(svar)
  }

  val restenAvSvarene = listOf(Svar("3.3", "title"), Svar("3.4", "ja"))

  @Test
  @Order(4)
  @DisplayName(
      "vi skal kunne legge til flere svar på et testresultat vi har opprettet, og hente dem ut igjen")
  fun leggeTilFlereSvar() {
    val resultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endret = resultat.copy(svar = resultat.svar + restenAvSvarene)
    restTemplate.put(location, endret)

    val oppdatertResultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatertResultat.svar).containsExactlyElementsOf(svar + restenAvSvarene)
  }

  @Test
  @Order(5)
  @DisplayName("vi skal kunne oppdatere et svar")
  fun oppdatereSvar() {
    val resultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endretSvar = resultat.svar.map { if (it.steg == "3.4") it.copy(svar = "nei") else it }
    restTemplate.put(location, resultat.copy(svar = endretSvar))

    val oppdatertResultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatertResultat.svar).contains(Svar("3.4", "nei"))
    assertThat(oppdatertResultat.svar).doesNotContain(Svar("3.4", "ja"))
  }

  @Test
  @Order(6)
  @DisplayName("vi skal kunne oppdatere testresultatet med elementomtale")
  fun oppdatereTestresultat() {
    val testresultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endret = testresultat.copy(elementOmtale = "iframe nummer 1")

    restTemplate.put(location, endret)

    val oppdatert = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatert.elementOmtale).isEqualTo("iframe nummer 1")
  }

  @Test
  @Order(7)
  @DisplayName(
      "når vi oppdaterer resultatet med elementresultat og elementutfall, så skal også tidspunktet settes")
  fun oppdatereTestresultatMedElementresultatOgElementutfall() {
    val start = Instant.now()
    val testresultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val elementUtfall =
        "Iframe har et tilgjengelig navn, som ikke beskriver formålet med innholdet i iframe."
    val elementResultat = "brudd"
    val endret = testresultat.copy(elementResultat = elementResultat, elementUtfall = elementUtfall)

    restTemplate.put(location, endret)

    val oppdatert = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatert.elementResultat).isEqualTo(elementResultat)
    assertThat(oppdatert.elementUtfall).isEqualTo(elementUtfall)
    assertThat(oppdatert.testVartUtfoert).isBetween(start, Instant.now())
  }

  @Test
  @Order(8)
  @DisplayName("vi kan hente alle resultater for en gitt sak")
  fun henteAlleResultaterForSak() {
    val resultatForSak =
        restTemplate.getForObject("/testresultat?sakId=$sakId", ResultatForSak::class.java)!!
    assertThat(resultatForSak.resultat).hasSize(1)
    val resultat = resultatForSak.resultat.first()
    assertThat(resultat.elementOmtale).isEqualTo("iframe nummer 1")
    assertThat(resultat.elementResultat).isEqualTo("brudd")
    assertThat(resultat.elementUtfall)
        .isEqualTo(
            "Iframe har et tilgjengelig navn, som ikke beskriver formålet med innholdet i iframe.")
    assertThat(resultat.testVartUtfoert).isNotNull()
    val expected =
        (svar + restenAvSvarene).map { if (it.steg == "3.4") it.copy(svar = "nei") else it }
    assertThat(resultat.svar).containsExactlyElementsOf(expected)
  }

  data class ResultatForSak(val resultat: List<ResultatManuellKontroll>)
}
