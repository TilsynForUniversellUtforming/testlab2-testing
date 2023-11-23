package no.uutilsynet.testlab2testing.inngaendekontroll

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
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
      val result = restTemplate.postForEntity("/saker", virksomhet, Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.CREATED)
      assertThat(result.headers.location).isNotNull()
    }

    @DisplayName("når vi sender inn eit ugyldig orgnummer, så skal vi bli avvist")
    @Test
    fun ugyldigOrgnummer() {
      val virksomhet = "123456789"
      val result = restTemplate.postForEntity("/saker", virksomhet, Unit::class.java)
      assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @DisplayName("når vi har oppretta ei sak, så skal vi kunne hente den ut på adressa i location")
    @Test
    fun hentSak() {
      val virksomhet = "123456785"
      val result = restTemplate.postForEntity("/saker", virksomhet, Unit::class.java)
      val sak: Sak = restTemplate.getForObject(result.headers.location!!, Sak::class.java)!!
      assertThat(sak.virksomhet).isEqualTo(virksomhet)
    }
  }
}
