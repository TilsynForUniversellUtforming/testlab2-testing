package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelRequestBody
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKrav
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravtilsamsvar
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestReferanseAct
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestregelIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val testregelDAO: TestregelDAO
) {

  @AfterAll
  fun cleanup() {
    testregelDAO.jdbcTemplate.update(
        "delete from testregel where kravtilsamsvar = :kravtilsamsvar",
        mapOf("kravtilsamsvar" to testregelTestKravtilsamsvar))
  }

  @Test
  @DisplayName("Skal kunne opprette en testregel")
  fun createTestregel() {
    val locationPattern = """/v1/testregel/\d+"""
    val location = restTemplate.postForLocation("/v1/testregel", testregelRequestBody)
    Assertions.assertThat(location.toString()).matches(locationPattern)
  }

  @Test
  @DisplayName("Skal kunne slette testregel")
  fun deleteTestregel() {
    val location = createDefaultTestregel()
    val testregel = restTemplate.getForObject(location, Testregel::class.java)

    Assertions.assertThat(testregel).isNotNull
    restTemplate.delete(location)

    val deletedTestregel = restTemplate.getForObject(location, Testregel::class.java)
    Assertions.assertThat(deletedTestregel).isNull()
  }

  @Test
  @DisplayName("Skal kunne oppdatere testregel")
  fun updateTestregel() {
    val location = createDefaultTestregel()
    val testregel = restTemplate.getForObject(location, Testregel::class.java)
    val krav = "4.1.3 Statusbeskjeder"
    Assertions.assertThat(testregel.krav).isNotEqualTo(krav)

    restTemplate.exchange(
        "/v1/testregel", HttpMethod.PUT, HttpEntity(testregel.copy(krav = krav)), Int::class.java)

    val updatedTestregel = restTemplate.getForObject(location, Testregel::class.java)
    Assertions.assertThat(updatedTestregel.krav).isEqualTo(krav)
  }

  @Nested
  @DisplayName("Hvis det finnes en testregel i databasen")
  inner class DatabaseHasAtLeastOneTestregel(@Autowired val restTemplate: TestRestTemplate) {
    val location = createDefaultTestregel()

    @Test
    @DisplayName("Skal hente testregel")
    fun getTestregel() {
      val testregel = restTemplate.getForObject(location, Testregel::class.java)
      Assertions.assertThat(testregel.krav).isEqualTo(testregelTestKrav)
      Assertions.assertThat(testregel.referanseAct).isEqualTo(testregelTestReferanseAct)
      Assertions.assertThat(testregel.kravTilSamsvar).isEqualTo(testregelTestKravtilsamsvar)
    }

    @Test
    @DisplayName("Skal hente liste med testregel")
    fun getTestregelList() {
      val testregel = restTemplate.getForObject(location, Testregel::class.java)

      val testregelListType = object : ParameterizedTypeReference<List<Testregel>>() {}

      val testregelFromList =
          restTemplate
              .exchange("/v1/testregel", HttpMethod.GET, HttpEntity.EMPTY, testregelListType)
              .body
              ?.find { it.id == testregel.id }

      Assertions.assertThat(testregelFromList).isEqualTo(testregel)
    }
  }

  private fun createDefaultTestregel(): URI =
      restTemplate.postForLocation("/v1/testregel", testregelRequestBody)
}
