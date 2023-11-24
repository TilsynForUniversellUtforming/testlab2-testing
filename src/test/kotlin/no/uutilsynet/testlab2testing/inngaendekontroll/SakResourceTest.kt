package no.uutilsynet.testlab2testing.inngaendekontroll

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SakResourceTest(@Autowired val restTemplate: TestRestTemplate) {
  @DisplayName("oppretting av ei sak")
  @Nested
  inner class OpprettingAvEiSak {
    @DisplayName("når vi sender inn eit gyldig orgnummer, så skal vi få oppretta ei sak")
    @Test
    fun opprettingAvEiSak() {
      val virksomhet = "123456785"
      val result =
          restTemplate.postForEntity("/saker", mapOf("virksomhet" to virksomhet), Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(result.headers.location).isNotNull()
    }

    @DisplayName("når vi sender inn eit ugyldig orgnummer, så skal vi bli avvist")
    @Test
    fun ugyldigOrgnummer() {
      val virksomhet = "123456789"
      val result =
          restTemplate.postForEntity("/saker", mapOf("virksomhet" to virksomhet), Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @DisplayName("når vi har oppretta ei sak, så skal vi kunne hente den ut på adressa i location")
    @Test
    fun hentSak() {
      val virksomhet = "123456785"
      val result =
          restTemplate.postForEntity("/saker", mapOf("virksomhet" to virksomhet), Unit::class.java)
      val sak: Sak = restTemplate.getForObject(result.headers.location!!, Sak::class.java)!!
      assertThat(sak.virksomhet).isEqualTo(virksomhet)
    }
  }

  @DisplayName("oppdatering av ei sak")
  @Nested
  inner class OppdateringAvEiSak {
    @DisplayName("vi skal kunne legge til løysingar på ei sak")
    @Test
    fun leggTilEiLoeysing() {
      val location = restTemplate.postForLocation("/saker", mapOf("virksomhet" to "123456785"))!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val sakMedLoeysingar: Sak = sak.copy(loeysingar = listOf(Sak.Loeysing(1), Sak.Loeysing(2)))
      restTemplate.put(location, sakMedLoeysingar)

      val sakEtterOppdatering: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdatering.loeysingar).hasSize(2)
    }

    @DisplayName("vi skal kunne fjerne løysingar på ei sak")
    @Test
    fun fjerneLoeysingar() {
      val location = restTemplate.postForLocation("/saker", mapOf("virksomhet" to "123456785"))!!
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
      val location = restTemplate.postForLocation("/saker", mapOf("virksomhet" to "123456785"))!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val forside = Sak.Nettside("forside", "https://www.uutilsynet.no/", "Forsida", "")
      val artikkel =
          Sak.Nettside(
              "artikkel",
              "https://www.uutilsynet.no/tilsyn/slik-forer-vi-tilsyn-med-nettsteder-og-apper/84",
              "Slik fører vi tilsyn med nettsteder og apper",
              "")

      val loeysingMedNettsider = Sak.Loeysing(1, listOf(forside, artikkel))
      val sakMedLoeysingOgNettsider = sak.copy(loeysingar = listOf(loeysingMedNettsider))
      restTemplate.put(location, sakMedLoeysingOgNettsider)

      val sakEtterOppdateringAvLoeysing: Sak = restTemplate.getForObject(location)!!
      assertThat(sakEtterOppdateringAvLoeysing.loeysingar).hasSize(1)
      assertThat(sakEtterOppdateringAvLoeysing.loeysingar.first().nettsider)
          .containsExactlyInAnyOrder(forside, artikkel)
    }

    @DisplayName("vi skal kunne fjerne nettsider frå ei løysing")
    @Test
    fun fjernNettside() {
      val location = restTemplate.postForLocation("/saker", mapOf("virksomhet" to "123456785"))!!
      val sak: Sak = restTemplate.getForObject(location)!!

      val forside = Sak.Nettside("forside", "https://www.uutilsynet.no/", "Forsida", "")
      val artikkel =
          Sak.Nettside(
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
      assertThat(sakEtterFjerningAvNettside.loeysingar.first().nettsider).containsExactly(forside)
    }
  }
}
