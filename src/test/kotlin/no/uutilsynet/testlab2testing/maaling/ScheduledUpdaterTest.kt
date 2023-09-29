package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.maaling.ScheduledUpdater.Companion.updateCrawlingStatus
import no.uutilsynet.testlab2testing.maaling.ScheduledUpdater.Companion.updateTestingStatus
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.Mockito.anyList
import org.mockito.Mockito.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        CrawlResultat.IkkeFerdig(
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
          updateCrawlingStatus(
              crawlResultat, Result.failure(RuntimeException("500 Internal Server Error")))
    }

    assertThat(updatedCrawlResultat).isInstanceOf(CrawlResultat.Feilet::class.java)
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

  @ParameterizedTest
  @MethodSource("crawlOutputList")
  @DisplayName(
      "når vi oppdaterer ei måling med status Crawling til Kvalitetssikring, så skal riktig data lagres og returneres")
  fun updateIkkeFerdigToKvalitetssikring(
      crawlerOutput: List<CrawlerOutput>,
      expectedAntallNettsider: Int
  ) {
    val updater = ScheduledUpdater(maalingDAO, crawlerClient, autoTesterClient)

    val crawlResultatIkkeFerdig =
        CrawlResultat.IkkeFerdig(
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = uutilsynetLoeysing.url,
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now(),
            framgang = Framgang(0, 0))

    `when`(crawlerClient.getStatus(crawlResultatIkkeFerdig))
        .thenReturn(Result.success(CrawlStatus.Completed(crawlerOutput)))

    val maaling =
        Maaling.Crawling(
            id = 1,
            crawlResultat = listOf(crawlResultatIkkeFerdig),
            navn = "Test",
            datoStart = LocalDate.now())

    val updatedMaaling = updater.updateCrawlingStatuses(maaling)

    assertThat(updatedMaaling).isInstanceOf(Maaling.Kvalitetssikring::class.java)
    val crawlResultat = (updatedMaaling as Maaling.Kvalitetssikring).crawlResultat.first()
    assertThat(crawlResultat).isInstanceOf(CrawlResultat.Ferdig::class.java)
    assertThat((crawlResultat as CrawlResultat.Ferdig).antallNettsider)
        .isEqualTo(expectedAntallNettsider)

    verify(maalingDAO, times(1)).saveNettsider(eq(1), eq(1), anyList())
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
            id = 1, crawlResultat = crawlResultat, navn = "Test", datoStart = LocalDate.now())

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
            datoStart = LocalDate.now(),
            aksjoner = listOf())

    `when`(autoTesterClient.updateStatus(testKoeyring))
        .thenReturn(Result.success(AutoTesterClient.AutoTesterStatus.Pending))

    val updatedMaaling = updater.updateTestingStatuses(maaling)

    assertThat(updatedMaaling).isEqualTo(maaling)
  }

  companion object {
    @JvmStatic
    fun crawlOutputList(): Stream<Arguments> {
      return Stream.of(
          Arguments.of(
              listOf(
                  CrawlerOutput("https://www.uutilsynet.no/", "uutilsynet"),
                  CrawlerOutput("https://www.uutilsynet.no/side/1", "uutilsynet - side 1"),
              ),
              2),
          Arguments.of(
              listOf(
                  CrawlerOutput("https://www.uutilsynet.no/", "uutilsynet"),
                  CrawlerOutput("https://www.uutilsynet.no/[VIEWURL]", "ugylig_url")),
              1))
    }
  }
}
