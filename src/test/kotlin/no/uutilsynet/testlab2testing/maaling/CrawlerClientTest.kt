package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URL
import no.uutilsynet.testlab2testing.dto.Loeysing
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
            Loeysing(1, "uutilsynet", URL("https://www.uutilsynet.no")),
            Loeysing(2, "digdir", URL("https://www.digdir.no")))
    val maaling = Maaling.Planlegging(1, "testmåling", loeysingList, CrawlParameters())
    val oppdatertMaaling = crawlerClient.start(maaling)

    assertThat(oppdatertMaaling.crawlResultat).hasSize(2)
    assertThat(oppdatertMaaling.crawlResultat)
        .hasOnlyElementsOfType(CrawlResultat.IkkeFerdig::class.java)
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
