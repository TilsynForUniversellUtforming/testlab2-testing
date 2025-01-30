package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URI
import java.time.Instant
import kotlin.properties.Delegates
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.Sakstype
import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.NyttTestgrunnlag
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase.Svar
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.kontroll.KontrollResource
import no.uutilsynet.testlab2testing.kontroll.SideutvalBase
import no.uutilsynet.testlab2testing.loeysing.UtvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class TestResultatResourceTest(
    @Autowired val kontrollDAO: KontrollDAO,
    @Autowired val testregelDAO: TestregelDAO,
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val testgrunnlagDAO: TestgrunnlagDAO
) {
  private var kontrollId: Int by Delegates.notNull()
  private var utvalId: Int by Delegates.notNull()
  private var location: URI by Delegates.notNull()
  private var testgrunnlagId: Int by Delegates.notNull()

  @MockitoSpyBean lateinit var brukarService: BrukarService

  @AfterAll
  fun cleanup() {
    utvalDAO.deleteUtval(utvalId)
  }

  @Test
  @Order(1)
  @DisplayName("vi skal kunne opprette et nytt testresultat")
  fun nyttTestresultat() {
    val opprettKontroll =
        KontrollResource.OpprettKontroll(
            "manuell-kontroll",
            "Ola Nordmann",
            Sakstype.Arkivsak,
            "1234",
            Kontrolltype.InngaaendeKontroll)

    kontrollId = kontrollDAO.createKontroll(opprettKontroll).getOrThrow()

    doReturn(Brukar("testbrukar@digdir.no", "Test Brukar")).`when`(brukarService).getCurrentUser()

    brukarService.getUserId(Brukar("testbrukar@digdir.no", "Test Brukar"))

    val kontroll =
        Kontroll(
            kontrollId,
            Kontrolltype.InngaaendeKontroll,
            opprettKontroll.tittel,
            opprettKontroll.saksbehandler,
            opprettKontroll.sakstype,
            opprettKontroll.arkivreferanse,
        )

    /* Add utval */
    val loeysingId = 1
    utvalId = utvalDAO.createUtval("test-skal-slettes", listOf(loeysingId)).getOrThrow()
    kontrollDAO.updateKontroll(kontroll, utvalId)

    /* Add testreglar */
    val testregel = testregelDAO.getTestregelList().first()
    kontrollDAO.updateKontroll(kontroll, null, listOf(testregel.id))

    /* Add sideutval */
    kontrollDAO.updateKontroll(
        kontroll,
        listOf(
            SideutvalBase(loeysingId, 1, "Begrunnelse", URI.create("https://www.digdir.no"), null),
        ))

    val createdKontroll = kontrollDAO.getKontroller(listOf(kontrollId)).getOrThrow().first()
    val testregelId =
        createdKontroll.testreglar?.testregelIdList?.first()
            ?: throw IllegalArgumentException("Testregel finns ikkje")
    val sideutval = createdKontroll.sideutval.first()

    val nyttTestgrunnlag =
        NyttTestgrunnlag(
            kontrollId,
            "Testgrunnlag",
            TestgrunnlagType.OPPRINNELEG_TEST,
            listOf(sideutval),
            listOf(testregelId),
        )

    val testgrunnlag = testgrunnlagDAO.createTestgrunnlag(nyttTestgrunnlag)
    testgrunnlagId = testgrunnlag.getOrThrow()

    val responseEntity =
        restTemplate.postForEntity(
            "/testresultat",
            mapOf(
                "testgrunnlagId" to testgrunnlagId,
                "loeysingId" to loeysingId,
                "testregelId" to testregelId,
                "sideutvalId" to sideutval.id,
                "brukar" to mapOf("brukarnamn" to "testbrukar@digdir.no", "namn" to "Test Brukar"),
            ),
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
    val elementResultat = TestresultatUtfall.brot
    val endret = testresultat.copy(elementResultat = elementResultat, elementUtfall = elementUtfall)

    restTemplate.put(location, endret)

    val oppdatert = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatert.elementResultat).isEqualTo(elementResultat)
    assertThat(oppdatert.elementUtfall).isEqualTo(elementUtfall)
    assertThat(oppdatert.testVartUtfoert).isBetween(start, Instant.now())
  }

  @Test
  @Order(7)
  @DisplayName("vi skal kunne oppdatere testresultat med frivillig kommentar")
  fun oppdatereTestresultatMedKommentar() {
    val start = Instant.now()
    val testresultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val kommentar = "Dette var en bra test"
    val endret = testresultat.copy(kommentar = kommentar)

    restTemplate.put(location, endret)

    val oppdatert = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)

    assertThat(oppdatert.kommentar).isEqualTo(kommentar)
    assertThat(oppdatert.testVartUtfoert).isBetween(start, Instant.now())
  }

  @Test
  @Order(9)
  @DisplayName("vi kan hente alle resultater for et gitt tesgrunnlag")
  fun henteAlleResultaterForTestgrunnlag() {
    val resultatForTestgrunnlag =
        restTemplate.getForObject(
            "/testresultat?testgrunnlagId=$testgrunnlagId", ResultatForTestgrunnlag::class.java)!!
    assertThat(resultatForTestgrunnlag.resultat).hasSize(1)
    val resultat = resultatForTestgrunnlag.resultat.first()
    assertThat(resultat.elementOmtale).isEqualTo("iframe nummer 1")
    assertThat(resultat.elementResultat).isEqualTo(resultat.elementResultat)
    assertThat(resultat.elementUtfall)
        .isEqualTo(
            "Iframe har et tilgjengelig navn, som ikke beskriver formålet med innholdet i iframe.")
    assertThat(resultat.testVartUtfoert).isNotNull()
    val expected =
        (svar + restenAvSvarene).map { if (it.steg == "3.4") it.copy(svar = "nei") else it }
    assertThat(resultat.svar).containsExactlyElementsOf(expected)
  }

  @Test
  @Order(10)
  @DisplayName("vi skal ikke kunne slette et testresultat hvis status er 'Ferdig'")
  fun sletteFerdigTestresultat() {
    val resultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endret = resultat.copy(status = ResultatManuellKontrollBase.Status.Ferdig)
    restTemplate.put(location, endret)
    val responseEntity = restTemplate.exchange(location, HttpMethod.DELETE, null, Unit::class.java)
    assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  @Order(11)
  @DisplayName("vi skal kunne slette et testresultat hvis status er noe annet enn 'Ferdig'")
  fun sletteTestresultat() {
    val resultat = restTemplate.getForObject(location, ResultatManuellKontroll::class.java)
    val endret = resultat.copy(status = ResultatManuellKontrollBase.Status.UnderArbeid)
    restTemplate.put(location, endret)

    restTemplate.delete(location)
    val resultatForTestgrunnlag =
        restTemplate.getForObject(
            "/testresultat?testgrunnlagId=$testgrunnlagId", ResultatForTestgrunnlag::class.java)!!
    assertThat(resultatForTestgrunnlag.resultat).isEmpty()
  }

  data class ResultatForTestgrunnlag(val resultat: List<ResultatManuellKontroll>)
}
