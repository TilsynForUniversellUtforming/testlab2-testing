package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingRequestBody
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestName
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestOrgNummer
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestUrl
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.samePropertyValuesAs
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoeysingIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val loeysingDAO: LoeysingDAO
) {
  @AfterAll
  fun cleanup() {
    loeysingDAO.jdbcTemplate.update(
        "delete from loeysing where namn = :namn", mapOf("namn" to loeysingTestName))
  }

  @ParameterizedTest
  @ValueSource(strings = ["v2"])
  @DisplayName("Skal kunne opprette en løsning")
  fun createLoeysing(versjon: String) {
    val locationPattern = """/v2/loeysing/\d+"""
    val location = restTemplate.postForLocation("/$versjon/loeysing", loeysingRequestBody)
    Assertions.assertThat(location.toString()).matches(locationPattern)
  }

  @Test
  @DisplayName("Skal slette løsning")
  fun deleteLoeysing() {
    val location = createDefaultLoeysing()
    val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

    Assertions.assertThat(loeysing).isNotNull
    restTemplate.delete(location)

    val deletedLoeysing = restTemplate.getForObject(location, Loeysing::class.java)
    Assertions.assertThat(deletedLoeysing).isNull()
  }

  @ParameterizedTest
  @ValueSource(strings = ["v2"])
  @DisplayName("Skal oppdatere løsning")
  fun updateLoeysing(versjon: String) {
    val oldName = "test_skal_slettes_1"
    val oldUrl = "https://www.w3.org/"
    val oldOrgnummer = "012345674"

    val location =
        restTemplate.postForLocation(
            "/$versjon/loeysing",
            mapOf("namn" to oldName, "url" to oldUrl, "orgnummer" to oldOrgnummer))
    val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

    restTemplate.exchange(
        "/$versjon/loeysing",
        HttpMethod.PUT,
        HttpEntity(
            loeysing.copy(
                namn = loeysingTestName,
                url = URL(loeysingTestUrl),
                orgnummer = loeysingTestOrgNummer)),
        Int::class.java)

    val (_, namn, url, orgnummer) = restTemplate.getForObject(location, Loeysing::class.java)
    Assertions.assertThat(namn).isEqualTo(loeysingTestName)
    Assertions.assertThat(url.toString()).isEqualTo(loeysingTestUrl)
    if (orgnummer != null) {
      Assertions.assertThat(orgnummer).isEqualTo(loeysingTestOrgNummer)
    }
  }

  @Test
  @DisplayName("Skal kunne slette en løsning som ikke er brukt i måling")
  fun deleteLoeysingNotUsedInMaaling() {
    val location = createDefaultLoeysing()
    val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

    Assertions.assertThat(loeysing).isNotNull
    restTemplate.delete(location)

    val deletedLoeysing = restTemplate.getForObject(location, Loeysing::class.java)
    Assertions.assertThat(deletedLoeysing).isNull()
  }

  @Nested
  @DisplayName("Hvis det finnes en løsning i databasen")
  inner class DatabaseHasAtLeastOneLoeysing(@Autowired val restTemplate: TestRestTemplate) {
    private val location = createDefaultLoeysing()

    @Test
    @DisplayName("Skal hente løsning")
    fun getLoeysing() {
      val (_, namn, url) = restTemplate.getForObject(location, Loeysing::class.java)
      Assertions.assertThat(namn).isEqualTo(loeysingTestName)
      Assertions.assertThat(url.toString()).isEqualTo(loeysingTestUrl)
    }

    @Test
    @DisplayName("Skal hente liste med løsninger")
    fun getLoeysingList() {
      val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

      val loeysingListType = object : ParameterizedTypeReference<List<Loeysing>>() {}

      val loeysingFromList = assertDoesNotThrow {
        restTemplate
            .exchange("/v2/loeysing", HttpMethod.GET, HttpEntity.EMPTY, loeysingListType)
            .body
            ?.find { it.id == loeysing.id }!!
      }

      assertThat(loeysingFromList, samePropertyValuesAs(loeysing))
    }
  }

  private fun createDefaultLoeysing(): URI =
      restTemplate.postForLocation("/v2/loeysing", loeysingRequestBody)
}
