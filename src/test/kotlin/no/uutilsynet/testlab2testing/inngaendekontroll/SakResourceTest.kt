package no.uutilsynet.testlab2testing.inngaendekontroll

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
import org.springframework.boot.test.web.client.getForObject
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakResourceTest(@Autowired val restTemplate: TestRestTemplate) {
  val orgnummer: MutableList<String> = mutableListOf()

  @DisplayName("oppretting av ei sak")
  @Nested
  @TestMethodOrder(OrderAnnotation::class)
  inner class OpprettingAvEiSak {
    @DisplayName("når vi sender inn eit gyldig orgnummer, så skal vi få oppretta ei sak")
    @Test
    @Order(1)
    fun opprettingAvEiSak() {
      val virksomhet = tilfeldigOrgnummer()
      val namn = "Testheim kommune"
      orgnummer.add(virksomhet)
      val result =
          restTemplate.postForEntity(
              "/saker", mapOf("virksomhet" to virksomhet, "namn" to namn), Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(result.headers.location).isNotNull()
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

    @DisplayName("når vi har oppretta ei sak, så skal vi kunne hente den ut på adressa i location")
    @Test
    @Order(3)
    fun hentSak() {
      val virksomhet = tilfeldigOrgnummer()
      val namn = "Testheim kommune"
      orgnummer.add(virksomhet)
      val result =
          restTemplate.postForEntity(
              "/saker", mapOf("virksomhet" to virksomhet, "namn" to namn), Unit::class.java)
      val sak: Sak = restTemplate.getForObject(result.headers.location!!, Sak::class.java)!!
      assertThat(sak.virksomhet).isEqualTo(virksomhet)
      assertThat(sak.namn).isEqualTo(namn)
    }

    @DisplayName("når vi har oppretta to saker, så skal vi kunne liste dei ut")
    @Test
    @Order(4)
    fun listUtSaker() {
      val saker = restTemplate.getForObject("/saker", Array<SakListeElement>::class.java)!!
      assertThat(saker.size).isGreaterThanOrEqualTo(2)
      orgnummer.forEach { virksomhet ->
        assertThat(saker.any { it.virksomhet == virksomhet }).isTrue()
      }
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
