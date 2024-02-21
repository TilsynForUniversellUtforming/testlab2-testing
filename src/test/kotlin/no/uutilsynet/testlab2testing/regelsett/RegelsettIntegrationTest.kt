package no.uutilsynet.testlab2testing.regelsett

import java.net.URI
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettModus
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettName
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestCreateRequestBody
import no.uutilsynet.testlab2testing.testregel.TestregelModus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegelsettIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val regelsettDAO: RegelsettDAO,
) {

  val regelsettBaseUri = "/v1/regelsett"

  @AfterAll
  fun cleanup() {
    regelsettDAO.jdbcTemplate.update(
        "delete from regelsett where namn = :namn", mapOf("namn" to regelsettName))
  }

  @Test
  @DisplayName("Skal kunne opprette eit regelsett")
  fun createRegelsett() {
    val locationPattern = """/v1/regelsett/\d+"""
    val location = restTemplate.postForLocation(regelsettBaseUri, regelsettTestCreateRequestBody())

    assertThat(location.toString()).matches(locationPattern)
  }

  @Test
  @DisplayName("Skal ikkje kunne opprette eit regelsett med tomt namn")
  fun createRegelsettIllegalName() {
    val response =
        restTemplate.postForEntity(
            regelsettBaseUri, regelsettTestCreateRequestBody(namn = ""), String::class.java)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo("mangler navn")
    assertThat(response.headers.location).isNull()
  }

  @Test
  @DisplayName("Skal ikkje kunne opprette eit regelsett med andre typar testreglar enn regelsettet")
  fun createRegelsettIllegalTestregelType() {
    val response =
        restTemplate.postForEntity(
            regelsettBaseUri,
            regelsettTestCreateRequestBody(modus = TestregelModus.manuell),
            String::class.java)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).contains("Id-ane 1, 2 er ikkje gyldige")
    assertThat(response.headers.location).isNull()
  }

  @Test
  @DisplayName("Skal kunne hente ei liste med aktive regelsett")
  fun getRegelsettList() {
    val regelsettType = object : ParameterizedTypeReference<List<RegelsettBase>>() {}
    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)
    val responseIdList =
        restTemplate
            .exchange(regelsettBaseUri, HttpMethod.GET, HttpEntity.EMPTY, regelsettType)
            .body
            ?.map { it.id }

    assertThat(responseIdList).contains(regelsett.id)
  }

  @Test
  @DisplayName("Skal kunne hente ei liste med regelsett med testreglar")
  fun getRegelsettListWithTestreglar() {
    val regelsettType = object : ParameterizedTypeReference<List<RegelsettResponse>>() {}
    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)
    val response =
        restTemplate
            .exchange(
                "$regelsettBaseUri?includeTestreglar=true",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                regelsettType)
            .body

    assertThat(response?.get(0)?.testregelList).isNotEmpty
    assertThat(response?.map { it.id }).contains(regelsett.id)
  }

  @Test
  @DisplayName("Skal kunne hente ei liste med både aktive og inaktive regelsett")
  fun getRegelsettListActiveInactive() {
    val regelsettType = object : ParameterizedTypeReference<List<RegelsettBase>>() {}
    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)

    restTemplate.exchange<Unit>(
        "$regelsettBaseUri/${regelsett.id}", HttpMethod.DELETE, HttpEntity.EMPTY)

    val responseActiveIdList =
        restTemplate
            .exchange(regelsettBaseUri, HttpMethod.GET, HttpEntity.EMPTY, regelsettType)
            .body
            ?.map { it.id }

    assertThat(responseActiveIdList).doesNotContain(regelsett.id)

    val responseAllIdList =
        restTemplate
            .exchange(
                "$regelsettBaseUri?includeInactive=true",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                regelsettType)
            .body
            ?.map { it.id }

    assertThat(responseAllIdList).contains(regelsett.id)
  }

  @Test
  @DisplayName("Skal kunne oppdatere eit regelsett")
  fun updateRegelsett() {
    val name = "${regelsettName}_1"
    val nameUpdate = regelsettName

    val location = createDefaultRegelsett(namn = name)
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)
    assertThat(regelsett.namn).isEqualTo(name)

    restTemplate.exchange(
        regelsettBaseUri,
        HttpMethod.PUT,
        HttpEntity(
            RegelsettEdit(
                id = regelsett.id,
                namn = nameUpdate,
                modus = regelsett.modus,
                standard = regelsett.standard,
                testregelIdList = regelsett.testregelList.map { it.id })),
        Unit::class.java)

    val regelsettAfterUpdate = restTemplate.getForObject(location, RegelsettResponse::class.java)

    assertThat(regelsettAfterUpdate.namn).isEqualTo(nameUpdate)
  }

  @Test
  @DisplayName("Skal ikkje kunne oppdatere eit regelsett med ulovlig namn")
  fun updateRegelsettIllegalName() {
    val nameUpdate = ""

    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)
    assertThat(regelsett.namn).isEqualTo(regelsettName)

    val response =
        restTemplate.exchange(
            regelsettBaseUri,
            HttpMethod.PUT,
            HttpEntity(
                RegelsettEdit(
                    id = regelsett.id,
                    namn = nameUpdate,
                    modus = regelsett.modus,
                    standard = regelsett.standard,
                    testregelIdList = regelsett.testregelList.map { it.id })),
            String::class.java)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).isEqualTo("mangler navn")
  }

  @Test
  @DisplayName(
      "Skal ikkje kunne oppdatere eit regelsett til ein annan type enn typen til testrelgane, eit regelsett og dets typar må vera same type")
  fun updateRegelsettIllegalTestregelType() {
    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)
    assertThat(regelsett.modus).isEqualTo(TestregelModus.forenklet)

    val response =
        restTemplate.exchange(
            regelsettBaseUri,
            HttpMethod.PUT,
            HttpEntity(
                RegelsettEdit(
                    id = regelsett.id,
                    namn = regelsett.namn,
                    modus = TestregelModus.manuell,
                    standard = regelsett.standard,
                    testregelIdList = regelsett.testregelList.map { it.id })),
            String::class.java)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body).contains("Id-ane 1, 2 er ikkje gyldige")
  }

  @Test
  @DisplayName(
      "Hvis ein slettar (setter inaktivt) eit regelsett, skal det ikkje kome opp i lista hvis ikkje annna er spesifisert")
  fun deleteRegelsett() {
    val regelsettType = object : ParameterizedTypeReference<List<RegelsettBase>>() {}
    val location = createDefaultRegelsett()
    val regelsett = restTemplate.getForObject(location, RegelsettResponse::class.java)

    val responseActiveIdList =
        restTemplate
            .exchange(regelsettBaseUri, HttpMethod.GET, HttpEntity.EMPTY, regelsettType)
            .body
            ?.map { it.id }

    assertThat(responseActiveIdList).contains(regelsett.id)

    restTemplate.exchange<Unit>(
        "$regelsettBaseUri/${regelsett.id}", HttpMethod.DELETE, HttpEntity.EMPTY)

    val responseAllIdList =
        restTemplate
            .exchange(regelsettBaseUri, HttpMethod.GET, HttpEntity.EMPTY, regelsettType)
            .body
            ?.map { it.id }

    assertThat(responseAllIdList).doesNotContain(regelsett.id)
  }

  private fun createDefaultRegelsett(
      namn: String = regelsettName,
      type: TestregelModus = regelsettModus,
      standard: Boolean = RegelsettTestConstants.regelsettStandard,
      testregelIdList: List<Int> = RegelsettTestConstants.regelsettTestregelIdList,
  ): URI =
      restTemplate.postForLocation(
          regelsettBaseUri,
          regelsettTestCreateRequestBody(
              namn,
              type,
              standard,
              testregelIdList,
          ))
}
