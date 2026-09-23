package no.uutilsynet.testlab2testing.regelsett

import java.net.URI
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettModus
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettName
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestCreateRequestBody
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelIdList
import no.uutilsynet.testlab2testing.regelsett.RegelsettTestConstants.regelsettTestregelList
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.returnResult
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.datasource.url= jdbc:tc:postgresql:16-alpine:///RegelsettTest-db"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class RegelsettIntegrationTest(
    @Autowired val restTemplate: TestRestTemplate,
    @Autowired val regelsettDAO: RegelsettDAO,
) {

    @MockitoBean
    lateinit var testregelClient: TestregelClient

    val regelsettBaseUri = "/v1/regelsett"

    lateinit var client: RestTestClient


    @BeforeEach
    fun setup(context: WebApplicationContext) {
        client = RestTestClient.bindToApplicationContext(context).build()

        doReturn(Result.success(regelsettTestregelList))
            .`when`(testregelClient)
            .getTestregelListFromIds(regelsettTestregelIdList)

        doReturn(Result.success(regelsettTestregelList)).`when`(testregelClient).getTestregelList()
    }

    @AfterAll
    fun cleanup() {
        regelsettDAO.jdbcTemplate.update(
            "delete from regelsett where namn = :namn", mapOf("namn" to regelsettName)
        )
    }

    @Test
    @DisplayName("Skal kunne opprette eit regelsett")
    fun createRegelsett() {
        val locationPattern = """/v1/regelsett/\d+"""

        val location = client.post()
            .uri(regelsettBaseUri)
            .body(regelsettTestCreateRequestBody())
            .exchange()
            .expectStatus().isCreated
            .returnResult().responseHeaders.location


        assertThat(location.toString()).matches(locationPattern)
    }

    @Test
    @DisplayName("Skal ikkje kunne opprette eit regelsett med tomt namn")
    fun createRegelsettIllegalName() {


        val response = client.post()
            .uri(regelsettBaseUri)
            .body(regelsettTestCreateRequestBody(namn = ""))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().doesNotExist("Location")
            .returnResult<String>()
            .responseBody


        assertThat(response).isEqualTo("mangler navn")

    }

    @Test
    @DisplayName("Skal ikkje kunne opprette eit regelsett med andre typar testreglar enn regelsettet")
    fun createRegelsettIllegalTestregelType() {

        val response = client.post()
            .uri(regelsettBaseUri)
            .body(regelsettTestCreateRequestBody(modus = TestregelModus.manuell))
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().doesNotExist("Location")
            .returnResult<String>()
            .responseBody


        assertThat(response).contains("Id-ane 1, 2 er ikkje gyldige")
    }

    @Test
    @DisplayName("Skal kunne hente ei liste med aktive regelsett")
    fun getRegelsettList() {
        val location = createDefaultRegelsett()

        val regelsett = getRegelsettResponse(location)

        val responseIdList = client.get()
            .uri(regelsettBaseUri)
            .exchange()
            .expectStatus().isOk
            .returnResult<List<RegelsettBase>>()
            .responseBody!!
            .map { it.id }


        assertThat(responseIdList).contains(regelsett.id)
    }

    @Test
    @DisplayName("Skal kunne hente ei liste med regelsett med testreglar")
    fun getRegelsettListWithTestreglar() {
        val location = createDefaultRegelsett()


        val regelsett = getRegelsettResponse(location)

        val response = client.get()
            .uri("$regelsettBaseUri?includeTestreglar=true")
            .exchange()
            .expectStatus().isOk
            .returnResult<List<RegelsettResponse>>()
            .responseBody!!


        assertThat(response.get(0).testregelList).isNotEmpty
        assertThat(response.map { it.id }).contains(regelsett.id)
    }


    @Test
    @DisplayName("Skal kunne hente ei liste med både aktive og inaktive regelsett")
    fun getRegelsettListActiveInactive() {
        val location = createDefaultRegelsett()
        val regelsett = getRegelsettResponse(location)

        client.delete().uri("$regelsettBaseUri/${regelsett.id}").exchange().expectStatus().isNoContent

        val responseActiveIdList = client.get().uri(regelsettBaseUri).exchange().expectStatus().isOk
            .returnResult<List<RegelsettBase>>().responseBody!!.map { it.id }


        assertThat(responseActiveIdList).doesNotContain(regelsett.id)

        val responseAllIdList =
            client.get().uri("$regelsettBaseUri?includeInactive=true").exchange().expectStatus().isOk
                .returnResult<List<RegelsettBase>>().responseBody!!.map { it.id }



        assertThat(responseAllIdList).contains(regelsett.id)
    }

    @Test
    @DisplayName("Skal kunne oppdatere eit regelsett")
    fun updateRegelsett() {
        val name = "${regelsettName}_1"
        val nameUpdate = regelsettName

        val location = createDefaultRegelsett(namn = name)
        val regelsett = getRegelsettResponse(location)
        assertThat(regelsett.namn).isEqualTo(name)

        client.put().uri(regelsettBaseUri)
            .body(
                RegelsettEdit(
                    id = regelsett.id,
                    namn = nameUpdate,
                    modus = regelsett.modus,
                    standard = regelsett.standard,
                    testregelIdList = regelsett.testregelList.map { it.id })
            )
            .exchange()
            .expectStatus().isOk


        val regelsettAfterUpdate = getRegelsettResponse(location)

        assertThat(regelsettAfterUpdate.namn).isEqualTo(nameUpdate)
    }

    @Test
    @DisplayName("Skal ikkje kunne oppdatere eit regelsett med ulovlig namn")
    fun updateRegelsettIllegalName() {
        val nameUpdate = ""

        val location = createDefaultRegelsett()
        val regelsett = getRegelsettResponse(location)
        assertThat(regelsett.namn).isEqualTo(regelsettName)

        val response = client.put()
            .uri(regelsettBaseUri)
            .body(
                RegelsettEdit(
                    id = regelsett.id,
                    namn = nameUpdate,
                    modus = regelsett.modus,
                    standard = regelsett.standard,
                    testregelIdList = regelsett.testregelList.map { it.id })
            )
            .exchange()
            .expectStatus().isBadRequest
            .returnResult<String>()
            .responseBody


        assertThat(response).isEqualTo("mangler navn")
    }

    @Test
    @DisplayName(
        "Skal ikkje kunne oppdatere eit regelsett til ein annan type enn typen til testrelgane, eit regelsett og dets typar må vera same type"
    )
    fun updateRegelsettIllegalTestregelType() {
        val location = createDefaultRegelsett()
        val regelsett = getRegelsettResponse(location)
        assertThat(regelsett.modus).isEqualTo(TestregelModus.automatisk)

        val response = client.put()
            .uri(regelsettBaseUri)
            .body(
                RegelsettEdit(
                    id = regelsett.id,
                    namn = regelsett.namn,
                    modus = TestregelModus.manuell,
                    standard = regelsett.standard,
                    testregelIdList = regelsett.testregelList.map { it.id })
            )
            .exchange()
            .expectStatus().isBadRequest
            .returnResult<String>()
            .responseBody

        assertThat(response).contains("Id-ane 1, 2 er ikkje gyldige")
    }

    @Test
    @DisplayName(
        "Hvis ein slettar (setter inaktivt) eit regelsett, skal det ikkje kome opp i lista hvis ikkje annna er spesifisert"
    )
    fun deleteRegelsett() {
        val regelsettType = object : ParameterizedTypeReference<List<RegelsettBase>>() {}
        val location = createDefaultRegelsett()
        val regelsett = getRegelsettResponse(location)

        val responseActiveIdList = client.get().uri(regelsettBaseUri).exchange().expectStatus().isOk
            .returnResult<List<RegelsettBase>>().responseBody!!.map { it.id }



        assertThat(responseActiveIdList).contains(regelsett.id)

        client.delete().uri("$regelsettBaseUri/${regelsett.id}").exchange().expectStatus().isNoContent


        val responseAllIdList = client.get().uri(regelsettBaseUri).exchange().expectStatus().isOk
            .returnResult<List<RegelsettBase>>().responseBody!!.map { it.id }



        assertThat(responseAllIdList).doesNotContain(regelsett.id)
    }

    private fun createDefaultRegelsett(
        namn: String = regelsettName,
        type: TestregelModus = regelsettModus,
        standard: Boolean = RegelsettTestConstants.regelsettStandard,
        testregelIdList: List<Int> = regelsettTestregelIdList,
    ): URI =

        client.post()
            .uri(regelsettBaseUri)
            .body(regelsettTestCreateRequestBody(namn, type, standard, testregelIdList))
            .exchange()
            .expectStatus().isCreated
            .returnResult().responseHeaders.location!!

    private fun getRegelsettResponse(location: URI): RegelsettResponse {
        val regelsett = client.get()
            .uri(location)
            .exchange()
            .expectStatus().isOk
            .returnResult<RegelsettResponse>()
            .responseBody!!
        return regelsett
    }
}
