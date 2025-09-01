package no.uutilsynet.testlab2testing.testregel

import java.net.URI
import no.uutilsynet.testlab2.constants.KravStatus
import no.uutilsynet.testlab2.constants.WcagPrinsipp
import no.uutilsynet.testlab2.constants.WcagRetninglinje
import no.uutilsynet.testlab2.constants.WcagSamsvarsnivaa
import no.uutilsynet.testlab2testing.krav.KravWcag2x
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.TestConstants.modus
import no.uutilsynet.testlab2testing.testregel.TestConstants.name
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelCreateRequestBody
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelSchemaAutomatisk
import no.uutilsynet.testlab2testing.testregel.TestConstants.testregelTestKravId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class TestregelIntegrationTests(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val testregelDAO: TestregelDAO,
) {

  @MockitoBean private lateinit var kravregisterClient: KravregisterClient

  @BeforeAll
  fun beforeAll() {
    Mockito.`when`(kravregisterClient.getWcagKrav(1))
        .thenReturn(
            KravWcag2x(
                1,
                "1.1.1 Ikke-tekstlig innhold,Gjeldande",
                KravStatus.gjeldande,
                "Innhald",
                false,
                false,
                false,
                "https://www.uutilsynet.no/wcag-standarden/111-ikke-tekstlig-innhold-niva/87",
                WcagPrinsipp.robust,
                WcagRetninglinje.leselig,
                "1.1.1",
                WcagSamsvarsnivaa.A,
                "kommentar"))
  }

  val deleteThese: MutableList<Int> = mutableListOf()

  @AfterAll
  fun cleanup() {
    deleteThese.forEach { testregelDAO.deleteTestregel(it) }
  }

  @Test
  @DisplayName("Skal kunne opprette en testregel")
  fun createTestregel() {
    val locationPattern = """/v1/testreglar/\d+"""
    val location = restTemplate.postForLocation("/v1/testreglar", testregelCreateRequestBody)

    deleteThese.add(idFromLocation(location))

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
                "testregelSchema" to testregelSchemaAutomatisk,
                "name" to name,
                "type" to "automatisk"),
            String::class.java)

    Assertions.assertThat(errorResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  @DisplayName("Skal ikke kunne opprette en testregel hvis krav ikke finnes")
  fun createTestregelKravError() {
    Mockito.`when`(kravregisterClient.getWcagKrav(1)).thenThrow(RuntimeException())

    val errorResponse =
        restTemplate.postForEntity("/v1/testreglar", testregelCreateRequestBody, String::class.java)

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
    private val location = createDefaultTestregel()

    @Test
    @DisplayName("Skal hente testregel")
    fun getTestregel() {
      val testregel = restTemplate.getForObject(location, Testregel::class.java)
      Assertions.assertThat(testregel.kravId).isEqualTo(testregelTestKravId)
      Assertions.assertThat(testregel.testregelSchema).isEqualTo(testregelSchemaAutomatisk)
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
      restTemplate.postForLocation("/v1/testreglar", testregelCreateRequestBody).also {
        deleteThese.add(idFromLocation(it))
      }

  private fun idFromLocation(location: URI) = location.path.split("/").last().toInt()
}
