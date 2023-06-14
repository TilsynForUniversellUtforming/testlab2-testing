package no.uutilsynet.testlab2testing.maaling

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO
import no.uutilsynet.testlab2testing.maaling.TestConstants.crawlResultat
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus

@RestClientTest
class MaalingResourceMockedTest {
  @MockBean private lateinit var maalingDAO: MaalingDAO

  @MockBean private lateinit var loeysingDAO: LoeysingDAO

  @MockBean private lateinit var testregelDAO: TestregelDAO

  @MockBean private lateinit var autoTesterClient: AutoTesterClient

  @MockBean private lateinit var crawlerClient: CrawlerClient

  private lateinit var maalingResource: MaalingResource

  @BeforeEach
  fun setup() {
    MockitoAnnotations.openMocks(this)
    maalingResource =
        MaalingResource(maalingDAO, loeysingDAO, testregelDAO, crawlerClient, autoTesterClient)
  }

  @Test
  @DisplayName("should return error when status is updated with invalid testregelNoekkel")
  fun illegalTestregelNoekkel() {
    val id = 1
    val status = MaalingResource.StatusDTO("testing", null)
    val maaling =
        Maaling.Kvalitetssikring(
            id = id, navn = "Testmaaling", crawlResultat = listOf(crawlResultat))

    `when`(maalingDAO.getMaaling(id)).thenReturn(maaling)
    `when`(testregelDAO.getTestreglarForMaaling(id))
        .thenReturn(Result.success(listOf(Testregel(1, "krav", "QW", "samsvar"))))

    val result = maalingResource.putNewStatus(id, status)

    Assertions.assertThat(result.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    Assertions.assertThat(result.body).isEqualTo("TestregelNoekkel må vera på formen QW-ACT-RXX")
  }
}
