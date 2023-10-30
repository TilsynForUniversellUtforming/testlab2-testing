package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingRequestBody
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestName
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestOrgNummer
import no.uutilsynet.testlab2testing.loeysing.TestConstants.loeysingTestUrl
import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.samePropertyValuesAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("skrudd av mens vi jobber med nytt løsningsregister")
class LoeysingIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val loeysingDAO: LoeysingDAO
) {
  @AfterEach
  fun cleanup() {
    loeysingDAO.jdbcTemplate.update(
        "delete from loeysing where namn = :namn", mapOf("namn" to loeysingTestName))
  }

  @Test
  @DisplayName("Skal kunne opprette en løsning")
  fun createLoeysing() {
    val locationPattern = """/v2/loeysing/\d+"""
    val location = restTemplate.postForLocation("/v2/loeysing", loeysingRequestBody)
    Assertions.assertThat(location.toString()).matches(locationPattern)
  }

  @Test
  @DisplayName("Skal kunne opprette en løsning")
  fun createLoeysingDuplicate() {
    val response =
        restTemplate.postForEntity("/v2/loeysing", loeysingRequestBody, String::class.java)
    assertThat(response.statusCode, Matchers.equalTo(HttpStatus.CREATED))

    val duplicateResponse =
        restTemplate.postForEntity("/v2/loeysing", loeysingRequestBody, String::class.java)
    assertThat(duplicateResponse.statusCode, Matchers.equalTo(HttpStatus.BAD_REQUEST))
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

  fun randomString(): String =
      (1..24).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")

  @Test
  @DisplayName("Skal oppdatere løsning")
  fun updateLoeysing() {
    val oldName = "test_skal_slettes_1"
    val oldUrl = "https://www.${randomString()}.org/"
    val oldOrgnummer = "012345674"

    val location =
        restTemplate.postForLocation(
            "/v2/loeysing", mapOf("namn" to oldName, "url" to oldUrl, "orgnummer" to oldOrgnummer))
    val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

    restTemplate.exchange(
        "/v2/loeysing",
        HttpMethod.PUT,
        HttpEntity(
            loeysing.copy(
                namn = loeysingTestName,
                url = URI(loeysingTestUrl).toURL(),
                orgnummer = loeysingTestOrgNummer)),
        Int::class.java)

    val (_, namn, url, orgnummer) = restTemplate.getForObject(location, Loeysing::class.java)
    Assertions.assertThat(namn).isEqualTo(loeysingTestName)
    Assertions.assertThat(url.toString()).isEqualTo(loeysingTestUrl)
    Assertions.assertThat(orgnummer).isEqualTo(loeysingTestOrgNummer)
  }

  @Test
  @DisplayName("Skal kunne endre en løsning til å ha samme url og orgnr")
  fun updateLoeysingDuplicate() {
    restTemplate.postForEntity("/v2/loeysing", loeysingRequestBody, String::class.java)

    val oldName = "test_skal_slettes_1"
    val oldUrl = "https://www.${randomString()}.org/"
    val oldOrgnummer = "012345674"

    val location =
        restTemplate.postForLocation(
            "/v2/loeysing", mapOf("namn" to oldName, "url" to oldUrl, "orgnummer" to oldOrgnummer))
    val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

    val duplicateResponse =
        restTemplate.exchange(
            "/v2/loeysing",
            HttpMethod.PUT,
            HttpEntity(
                loeysing.copy(
                    namn = loeysingTestName,
                    url = URI(loeysingTestUrl).toURL(),
                    orgnummer = loeysingTestOrgNummer)),
            String::class.java)

    assertThat(duplicateResponse.statusCode, Matchers.equalTo(HttpStatus.BAD_REQUEST))

    loeysingDAO.jdbcTemplate.update(
        "delete from loeysing where namn = :namn", mapOf("namn" to "test_skal_slettes_1"))
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

    @Test
    @DisplayName("Skal hente liste med løsninger basert på navn")
    fun getLoeysingListByName() {
      val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

      val loeysingListType = object : ParameterizedTypeReference<List<Loeysing>>() {}

      val loeysingFromList = assertDoesNotThrow {
        restTemplate
            .exchange(
                "/v2/loeysing?namn=${loeysing.namn.dropLast(1)}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                loeysingListType)
            .body
            ?.find { it.id == loeysing.id }!!
      }

      assertThat(loeysingFromList, samePropertyValuesAs(loeysing))
    }

    @Test
    @DisplayName("Skal hente liste med løsninger basert på orgnr")
    fun getLoeysingListByOrgnr() {
      val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

      val loeysingListType = object : ParameterizedTypeReference<List<Loeysing>>() {}

      val loeysingFromList = assertDoesNotThrow {
        restTemplate
            .exchange(
                "/v2/loeysing?orgnummer=${loeysing.orgnummer.dropLast(1)}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                loeysingListType)
            .body
            ?.find { it.id == loeysing.id }!!
      }

      assertThat(loeysingFromList, samePropertyValuesAs(loeysing))
    }

    @Test
    @DisplayName("Skal ikke kunne hente liste med løsninger basert på både orgnr og namn")
    fun getLoeysingListByOrgnrAndName() {
      val loeysing = restTemplate.getForObject(location, Loeysing::class.java)

      val loeysingGetStatus =
          assertDoesNotThrow {
                restTemplate.getForEntity(
                    "/v2/loeysing?orgnummer=${loeysing.orgnummer}&namn=${loeysing.namn}",
                    String::class.java)
              }
              .statusCode

      Assertions.assertThat(loeysingGetStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }
  }

  private fun createDefaultLoeysing(): URI =
      restTemplate.postForLocation("/v2/loeysing", loeysingRequestBody)
}
