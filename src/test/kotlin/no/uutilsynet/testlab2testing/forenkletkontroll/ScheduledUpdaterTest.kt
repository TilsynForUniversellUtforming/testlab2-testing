package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.forenkletkontroll.ScheduledUpdater.Companion.updateCrawlingStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.ScheduledUpdater.Companion.updateTestingStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.uutilsynetLoeysing
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class ScheduledUpdaterTest {
  private val maalingDAO = Mockito.mock(MaalingDAO::class.java)
  private val crawlerClient = Mockito.mock(CrawlerClient::class.java)
  private val autoTesterClient = Mockito.mock(AutoTesterClient::class.java)

  @Test
  @DisplayName(
      "når oppdatering av status feiler mer enn 12 ganger, så skal crawlresultatet settes som 'feilet'")
  fun crawlStatusFailed() {
    val crawlResultat =
        CrawlResultat.Starta(
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = URI("https://www.uutilsynet.no").toURL(),
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now(),
            framgang = Framgang(0, 0))

    var updatedCrawlResultat: CrawlResultat? = null
    for (i in 1..13) {
      updatedCrawlResultat =
          updateCrawlingStatus(crawlResultat) {
            Result.failure(RuntimeException("500 Internal Server Error"))
          }
    }

    assertThat(updatedCrawlResultat).isInstanceOf(CrawlResultat.Feila::class.java)
  }

  @Test
  @DisplayName(
      "når oppdatering av status feiler mer enn 12 ganger, så skal testkøyringa settes som 'feilet'")
  fun testKoeyringStatusFailed() {
    val crawlResultat =
        CrawlResultat.Ferdig(
            antallNettsider = 1,
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = URI("https://www.uutilsynet.no").toURL(),
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now())
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat = crawlResultat,
            sistOppdatert = Instant.now(),
            statusURL = URI("https://www.uutilsynet.no/status/1").toURL(),
            framgang = Framgang(0, 0))

    var updatedTestKoeyring: TestKoeyring? = null
    for (i in 1..13) {
      updatedTestKoeyring =
          updateTestingStatus(testKoeyring) {
            Result.failure(RuntimeException("500 Internal Server Error"))
          }
    }

    assertThat(updatedTestKoeyring).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  @Test
  @DisplayName(
      "når vi oppdaterer ei måling med status Crawling til Kvalitetssikring, så skal riktig data lagres og returneres")
  fun updateIkkeFerdigToKvalitetssikring() {
    val updater = ScheduledUpdater(maalingDAO, crawlerClient, autoTesterClient)
    val crawlerOutput =
        listOf(CrawlerOutput(uutilsynetLoeysing.url.toString(), uutilsynetLoeysing.namn))

    val crawlResultatIkkjeStarta =
        CrawlResultat.IkkjeStarta(
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = uutilsynetLoeysing.url,
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now())

    `when`(crawlerClient.getStatus(crawlResultatIkkjeStarta, 1))
        .thenReturn(Result.success(CrawlStatus.Completed(crawlerOutput)))

    val maaling =
        Maaling.Crawling(
            id = 1,
            crawlResultat = listOf(crawlResultatIkkjeStarta),
            navn = "Test",
            datoStart = Instant.now())

    val expectedCrawlResultat =
        CrawlResultat.Ferdig(
            antallNettsider = 1,
            statusUrl = crawlResultatIkkjeStarta.statusUrl,
            loeysing = crawlResultatIkkjeStarta.loeysing,
            sistOppdatert = Instant.now(),
            nettsider = crawlerOutput.map { URI(it.url).toURL() })

    val updatedMaaling = updater.updateCrawlingStatuses(maaling)

    assertThat(updatedMaaling).isInstanceOf(Maaling.Kvalitetssikring::class.java)
    val crawlResultat = (updatedMaaling as Maaling.Kvalitetssikring).crawlResultat.first()
    assertThat(crawlResultat).isInstanceOf(CrawlResultat.Ferdig::class.java)
    assertThat(crawlResultat.loeysing).isEqualTo(expectedCrawlResultat.loeysing)
    assertThat((crawlResultat as CrawlResultat.Ferdig).nettsider)
        .isEqualTo(expectedCrawlResultat.nettsider)
    assertThat(crawlResultat.antallNettsider).isEqualTo(expectedCrawlResultat.nettsider.size)
  }

  @Test
  @DisplayName(
      "når vi oppdaterer ei måling med crawlresultat som er ferdig, så skal vi få samme data tilbake")
  fun updateKvalitetssikring() {
    val updater = ScheduledUpdater(maalingDAO, crawlerClient, autoTesterClient)

    val crawlResultat =
        listOf(
            CrawlResultat.Ferdig(
                antallNettsider = 1,
                statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
                loeysing =
                    Loeysing(
                        namn = "UUTilsynet",
                        url = URI("https://www.uutilsynet.no").toURL(),
                        id = 1,
                        orgnummer = "000000000"),
                sistOppdatert = Instant.now()))
    val maaling =
        Maaling.Crawling(
            id = 1, crawlResultat = crawlResultat, navn = "Test", datoStart = Instant.now())

    val updatedMaaling = updater.updateCrawlingStatuses(maaling)

    assertThat(updatedMaaling).isInstanceOf(Maaling.Kvalitetssikring::class.java)
    assertThat((updatedMaaling as Maaling.Kvalitetssikring).crawlResultat).isEqualTo(crawlResultat)
  }

  @Test
  @DisplayName(
      "når vi oppdaterer en måling med testkjøringer som ikke har starta, så skal vi få samme data tilbake")
  fun updateTesting() {
    val updater = ScheduledUpdater(maalingDAO, crawlerClient, autoTesterClient)

    val crawlResultat =
        CrawlResultat.Ferdig(
            antallNettsider = 1,
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = URI("https://www.uutilsynet.no").toURL(),
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now())

    val testKoeyring =
        TestKoeyring.IkkjeStarta(
            crawlResultat = crawlResultat,
            sistOppdatert = Instant.now(),
            statusURL = URI("https://www.uutilsynet.no/status/1").toURL())
    val maaling =
        Maaling.Testing(
            id = 1,
            testKoeyringar = listOf(testKoeyring),
            navn = "Test",
            datoStart = Instant.now(),
            aksjoner = listOf())

    `when`(autoTesterClient.updateStatus(testKoeyring))
        .thenReturn(Result.success(AutoTesterClient.AutoTesterStatus.Pending))

    val updatedMaaling = updater.updateTestingStatuses(maaling)

    assertThat(updatedMaaling).isEqualTo(maaling)
  }
}
