package no.uutilsynet.testlab2testing.inngaendekontroll

import java.net.URI
import kotlin.properties.Delegates
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.SakListeElement
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.tilfeldigOrgnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.getForObject
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakResourceTest(@Autowired val restTemplate: TestRestTemplate) {
  var orgnummer: String by Delegates.notNull()

  @DisplayName("oppretting av ei sak")
  @Nested
  @TestMethodOrder(OrderAnnotation::class)
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class OpprettingAvEiSak {
    private var location: URI by Delegates.notNull()

    @DisplayName("vi skal kunne opprette ei sak")
    @Test
    @Order(1)
    fun opprettingAvEiSak() {
      val virksomhet = tilfeldigOrgnummer()
      val namn = "Testheim kommune"
      val result =
          restTemplate.postForEntity(
              "/saker", mapOf("virksomhet" to virksomhet, "namn" to namn), Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(result.headers.location).isNotNull()

      orgnummer = virksomhet
      location = result.headers.location!!
    }

    @DisplayName("når vi sender inn eit ugyldig orgnummer, så skal vi bli avvist")
    @Test
    @Order(2)
    fun ugyldigOrgnummer() {
      val virksomhet = "123456789"
      val result =
          restTemplate.postForEntity("/saker", mapOf("virksomhet" to virksomhet), Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    private val testesen = Brukar("testesen@digdir.no", "Test Testesen")

    @DisplayName("når vi har oppretta ei sak, så skal vi kunne oppdatere den med ein ansvarleg")
    @Test
    @Order(2)
    fun oppdaterMedAnsvarleg() {
      val sak: Sak = restTemplate.getForObject(location, Sak::class.java)!!
      val oppdatertSak = sak.copy(ansvarleg = testesen)
      val responseEntity: ResponseEntity<Unit> =
          restTemplate.exchange<Unit>(location, HttpMethod.PUT, HttpEntity(oppdatertSak))
      assertThat(responseEntity.statusCode.is2xxSuccessful).isTrue()
    }

    @DisplayName("når vi har oppretta ei sak, så skal vi kunne hente den ut på adressa i location")
    @Test
    @Order(3)
    fun hentSak() {
      val sak: Sak = restTemplate.getForObject(location, Sak::class.java)!!
      assertThat(sak.namn).isEqualTo("Testheim kommune")
      assertThat(sak.virksomhet).isNotNull()
      assertThat(sak.ansvarleg).isEqualTo(testesen)
    }

    @DisplayName("når vi har oppretta to saker, så skal vi kunne liste dei ut")
    @Test
    @Order(4)
    fun listUtSaker() {
      val saker = restTemplate.getForObject("/saker", Array<SakListeElement>::class.java)!!
      val sak = saker.find { it.virksomhet == orgnummer }
      assertThat(saker.size).isGreaterThanOrEqualTo(2)
      saker.forEach { assertThat(it.namn).isNotBlank() }
      assertThat(sak).isNotNull()
      assertThat(sak!!.ansvarleg).isEqualTo(testesen)
    }
  }

  @DisplayName("oppdatering av ei sak")
  @Nested
  inner class OppdateringAvEiSak {
    val nySak = mapOf("namn" to "Testheim kommune", "virksomhet" to tilfeldigOrgnummer())

    @DisplayName("vi skal kunne legge til løysingar på ei sak")
    @Test
    fun leggTilEiLoeysing() {
      val location = restTemplate.postForLocation("/saker", nySak)!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val sakMedLoeysingar: Sak = sak.copy(loeysingar = listOf(Sak.Loeysing(1), Sak.Loeysing(2)))
      restTemplate.put(location, sakMedLoeysingar)

      val sakEtterOppdatering: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdatering.loeysingar).hasSize(2)
    }

    @DisplayName("vi skal kunne fjerne løysingar på ei sak")
    @Test
    fun fjerneLoeysingar() {
      val location = restTemplate.postForLocation("/saker", nySak)!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val sakMedLoeysingar: Sak = sak.copy(loeysingar = listOf(Sak.Loeysing(1), Sak.Loeysing(2)))
      restTemplate.put(location, sakMedLoeysingar)

      val sakEtterOppdatering: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdatering.loeysingar).hasSize(2)

      val sakUtanLoeysingar: Sak = sak.copy(loeysingar = emptyList())
      restTemplate.put(location, sakUtanLoeysingar)

      val sakEtterFjerning: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterFjerning.loeysingar).isEmpty()
    }

    @DisplayName("vi skal kunne legge til nettsider på ei løysing")
    @Test
    fun leggTilNettside() {
      val location = restTemplate.postForLocation("/saker", nySak)!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val forside = Sak.Nettside(1, "forside", "https://www.uutilsynet.no/", "Forsida", "")
      val artikkel =
          Sak.Nettside(
              2,
              "artikkel",
              "https://www.uutilsynet.no/tilsyn/slik-forer-vi-tilsyn-med-nettsteder-og-apper/84",
              "Slik fører vi tilsyn med nettsteder og apper",
              "")

      val loeysingMedNettsider = Sak.Loeysing(1, listOf(forside, artikkel))
      val sakMedLoeysingOgNettsider = sak.copy(loeysingar = listOf(loeysingMedNettsider))
      restTemplate.put(location, sakMedLoeysingOgNettsider)

      val sakEtterOppdateringAvLoeysing: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdateringAvLoeysing.loeysingar).hasSize(1)
      assertThat(sakEtterOppdateringAvLoeysing.loeysingar.first().nettsider.sortedBy { it.type })
          .usingRecursiveComparison()
          .ignoringFields("id")
          .isEqualTo(listOf(artikkel, forside))
    }

    @DisplayName("vi skal kunne fjerne nettsider frå ei løysing")
    @Test
    fun fjernNettside() {
      val location = restTemplate.postForLocation("/saker", nySak)!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val forside = Sak.Nettside(1, "forside", "https://www.uutilsynet.no/", "Forsida", "")
      val artikkel =
          Sak.Nettside(
              2,
              "artikkel",
              "https://www.uutilsynet.no/tilsyn/slik-forer-vi-tilsyn-med-nettsteder-og-apper/84",
              "Slik fører vi tilsyn med nettsteder og apper",
              "")

      val loeysingMedNettsider = Sak.Loeysing(1, listOf(forside, artikkel))
      val sakMedLoeysingOgNettsider = sak.copy(loeysingar = listOf(loeysingMedNettsider))
      restTemplate.put(location, sakMedLoeysingOgNettsider)

      val loeysingMedEiNettside = Sak.Loeysing(1, listOf(forside))
      val oppdatertSak = sak.copy(loeysingar = listOf(loeysingMedEiNettside))
      restTemplate.put(location, oppdatertSak)

      val sakEtterFjerningAvNettside: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterFjerningAvNettside.loeysingar).hasSize(1)
      assertThat(sakEtterFjerningAvNettside.loeysingar.first().nettsider.first())
          .usingRecursiveComparison()
          .ignoringFields("id")
          .isEqualTo(forside)
    }

    @DisplayName("vi skal kunne legge til testreglar på ei sak")
    @Test
    fun leggeTilTestreglar() {
      val location = restTemplate.postForLocation("/saker", nySak)!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val testreglar =
          restTemplate
              .getForObject("/v1/testreglar", Array<Testregel>::class.java)!!
              .toList()
              .take(3)
      assert(testreglar.isNotEmpty()) // testreglar blir lagt i databasen i migrasjon V24.
      val oppdatertSak = sak.copy(testreglar = testreglar)
      restTemplate.put(location, oppdatertSak)

      val sakEtterOppdatering: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdatering.testreglar).containsExactlyInAnyOrderElementsOf(testreglar)
    }
  }
}
