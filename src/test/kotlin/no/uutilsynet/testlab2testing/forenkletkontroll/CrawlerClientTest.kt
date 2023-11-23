package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import kotlinx.coroutines.runBlocking
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.TestConstants
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.manyTimes
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@RestClientTest(CrawlerClient::class, CrawlerProperties::class)
@DisplayName("Crawler test")
class CrawlerClientTest {
  @Autowired private lateinit var server: MockRestServiceServer
  @Autowired private lateinit var crawlerClient: CrawlerClient

  @Test
  @DisplayName("når vi kaller crawleren, så får vi en liste med crawlresultater i retur")
  fun testStartCrawl() {
    server
        .expect(manyTimes(), requestTo(startsWith(crawlerClient.crawlerProperties.url)))
        .andRespond(withSuccess(successBody(), MediaType.APPLICATION_JSON))

    val loeysingList =
        listOf(
            Loeysing(1, "uutilsynet", URI("https://www.uutilsynet.no").toURL(), "123456785"),
            Loeysing(2, "digdir", URI("https://www.digdir.no").toURL(), "123456785"))

    val testregelList =
        listOf(
            Testregel(
                1,
                TestConstants.testregelTestKrav,
                "QW-ACT-12",
                TestConstants.testregelTestKravTilSamsvar,
            ))
    val maaling =
        Maaling.Planlegging(
            1, "testmåling", maalingDateStart, loeysingList, testregelList, CrawlParameters())
    runBlocking {
      val oppdatertMaaling = crawlerClient.start(maaling)

      assertThat(oppdatertMaaling.crawlResultat).hasSize(2)
      assertThat(oppdatertMaaling.crawlResultat)
          .hasOnlyElementsOfType(CrawlResultat.IkkjeStarta::class.java)
    }
  }

  private fun successBody(): String =
      jacksonObjectMapper()
          .writeValueAsString(
              mapOf(
                  "id" to "3be950d539004ff89c2d7c95e073f456",
                  "purgeHistoryDeleteUri" to "https://purge.uri",
                  "restartPostUri" to "https://restart.uri",
                  "resumePostUri" to "https://resume.uri",
                  "rewindPostUri" to "https://rewind.uri",
                  "sendEventPostUri" to "https://send.event.uri",
                  "statusQueryGetUri" to "https://status.query.uri",
                  "suspendPostUri" to "https://suspend.uri",
                  "terminatePostUri" to "https://terminate.uri"))
}
