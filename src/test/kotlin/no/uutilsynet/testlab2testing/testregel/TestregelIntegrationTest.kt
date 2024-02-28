package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import no.uutilsynet.testlab2testing.testregel.TestConstants.modus
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelCreateRequestBody
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaForenklet
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestregelIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val testregelDAO: TestregelDAO
) {

  @AfterAll
  fun cleanup() {
    testregelDAO.jdbcTemplate.update(
        "delete from testregel where namn = :name", mapOf("name" to name))
  }

  @Test
  @DisplayName("Skal kunne opprette en testregel")
  fun createTestregel() {
    val locationPattern = """/v1/testreglar/\d+"""
    val location = restTemplate.postForLocation("/v1/testreglar", testregelCreateRequestBody)
    Assertions.assertThat(location.toString()).matches(locationPattern)
  }

  @Test
  @DisplayName("Skal ikke kunne opprette en testregel med feil request")
  fun createTestregelErrors() {
    val errorResponse =
        restTemplate.postForEntity(
            "/v1/testreglar",
            mapOf(
                "kravId" to "1",
                "testregelSchema" to testregelSchemaForenklet,
                "name" to name,
                "type" to "forenklet"),
            String::class.java)

    Assertions.assertThat(errorResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  @DisplayName("Skal kunne slette testregel")
  fun deleteTestregel() {
    val location = createDefaultTestregel()
    val testregel = restTemplate.getForObject(location, TestregelBase::class.java)

    Assertions.assertThat(testregel).isNotNull
    restTemplate.delete(location)

    val deletedTestregel = restTemplate.getForObject(location, TestregelBase::class.java)
    Assertions.assertThat(deletedTestregel).isNull()
  }

  @Test
  @DisplayName("Skal kunne oppdatere testregel")
  fun updateTestregel() {
    val location = createDefaultTestregel()
    val testregel = restTemplate.getForObject(location, Testregel::class.java)
    val kravId = 2
    Assertions.assertThat(testregel.kravId).isNotEqualTo(kravId)

    restTemplate.exchange(
        "/v1/testreglar",
        HttpMethod.PUT,
        HttpEntity(testregel.copy(kravId = kravId)),
        Int::class.java)

    val updatedTestregel = restTemplate.getForObject(location, TestregelBase::class.java)
    Assertions.assertThat(updatedTestregel.kravId).isEqualTo(kravId)
  }

  @Nested
  @DisplayName("Hvis det finnes en testregel i databasen")
  inner class DatabaseHasAtLeastOneTestregel(@Autowired val restTemplate: TestRestTemplate) {
    val location = createDefaultTestregel()

    @Test
    @DisplayName("Skal hente testregel")
    fun getTestregel() {
      val testregel = restTemplate.getForObject(location, Testregel::class.java)
      Assertions.assertThat(testregel.kravId).isEqualTo(testregelTestKravId)
      Assertions.assertThat(testregel.testregelSchema).isEqualTo(testregelSchemaForenklet)
      Assertions.assertThat(testregel.namn).isEqualTo(name)
    }

    @Test
    @DisplayName("Skal hente liste med testregel")
    fun getTestregelList() {
      val testregel = restTemplate.getForObject(location, TestregelBase::class.java)

      val testregelListType = object : ParameterizedTypeReference<List<TestregelBase>>() {}

      val testregelFromList =
          restTemplate
              .exchange("/v1/testreglar", HttpMethod.GET, HttpEntity.EMPTY, testregelListType)
              .body
              ?.find { it.id == testregel.id }

      Assertions.assertThat(testregelFromList?.kravId).isEqualTo(testregelTestKravId)
      Assertions.assertThat(testregelFromList?.modus).isEqualTo(modus)
      Assertions.assertThat(testregelFromList?.namn).isEqualTo(name)
    }
  }

  private fun createDefaultTestregel(): URI =
      restTemplate.postForLocation("/v1/testreglar", testregelCreateRequestBody)
}
