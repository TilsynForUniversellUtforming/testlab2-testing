package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.time.Instant
import kotlinx.coroutines.*
import no.uutilsynet.testlab2.constants.TestregelInnholdstype
import no.uutilsynet.testlab2.constants.TestregelModus
import no.uutilsynet.testlab2.constants.TestregelStatus
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.maalingDateStart
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlerClient
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlerProperties
import no.uutilsynet.testlab2testing.testregel.TestConstants
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
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
            Loeysing(
                1,
                "uutilsynet",
                URI("https://www.uutilsynet.no").toURL(),
                "123456785",
                "uutilsynet"),
            Loeysing(2, "digdir", URI("https://www.digdir.no").toURL(), "123456785", "digdir"))

    val testregelList =
        listOf(
            Testregel(
                1,
                TestConstants.testregelSchemaAutomatisk,
                1,
                TestConstants.name,
                1,
                TestregelStatus.publisert,
                Instant.now(),
                TestregelInnholdstype.nett,
                TestregelModus.automatisk,
                TestlabLocale.nb,
                1,
                1,
                "Er rett",
                "QW-ACT-12",
                1))
    val maaling =
        Maaling.Planlegging(
            1,
            "testmåling",
            maalingDateStart,
            loeysingList,
            testregelList.map { it.toTestregelBase() },
            CrawlParameters())
    runBlocking {
      val oppdatertMaaling = start(maaling)

      assertThat(oppdatertMaaling.crawlResultat).hasSize(2)
      assertThat(oppdatertMaaling.crawlResultat)
          .hasOnlyElementsOfType(CrawlResultat.IkkjeStarta::class.java)
    }
  }

  suspend fun start(maaling: Maaling.Planlegging): Maaling.Crawling = coroutineScope {
    val deferreds: List<Deferred<CrawlResultat>> =
        maaling.loeysingList.map { loeysing ->
          async { crawlerClient.start(loeysing, maaling.crawlParameters) }
        }
    val crawlResultatList = deferreds.awaitAll()
    Maaling.toCrawling(maaling, crawlResultatList)
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
