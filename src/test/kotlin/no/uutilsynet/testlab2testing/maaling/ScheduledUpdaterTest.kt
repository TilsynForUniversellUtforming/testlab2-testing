package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.maaling.ScheduledUpdater.Companion.updateCrawlingStatus
import no.uutilsynet.testlab2testing.maaling.ScheduledUpdater.Companion.updateTestingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ScheduledUpdaterTest {
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
          updateCrawlingStatus(crawlResultat) {
            Result.failure(RuntimeException("500 Internal Server Error"))
          }
    }

    assertThat(updatedCrawlResultat).isInstanceOf(CrawlResultat.Feilet::class.java)
  }

  @Test
  @DisplayName(
      "når oppdatering av status feiler mer enn 12 ganger, så skal testkøyringa settes som 'feilet'")
  fun testKoeyringStatusFailed() {
    val crawlResultat =
        CrawlResultat.Ferdig(
            statusUrl = URI("https://www.uutilsynet.no/status/1").toURL(),
            loeysing =
                Loeysing(
                    namn = "UUTilsynet",
                    url = URI("https://www.uutilsynet.no").toURL(),
                    id = 1,
                    orgnummer = "000000000"),
            sistOppdatert = Instant.now(),
            nettsider = listOf(URI("https://www.uutilsynet.no").toURL()))
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
}
