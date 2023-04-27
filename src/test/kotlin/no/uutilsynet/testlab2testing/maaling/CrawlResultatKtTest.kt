package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("tester for CrawlResultat")
class CrawlResultatKtTest {
  @DisplayName(
      "når vi oppdaterer fra ikke ferdig til ferdig, så skal crawlresultatet ha en liste med nettsider")
  @Test
  fun toFerdig() {
    val ikkeFerdig =
        CrawlResultat.IkkeFerdig(
            URL("https://status.uri"),
            uutilsynetLoeysing,
            Instant.now(),
            CrawlResultat.Framgang(2, 2))
    val crawlerOutput =
        listOf(
            CrawlerOutput("https://www.uutilsynet.no/", "uutilsynet"),
            CrawlerOutput(
                "https://www.uutilsynet.no/veiledning/tilgjengelighetserklaering/1127",
                "uutilsynet"))
    val updated =
        updateStatus(ikkeFerdig, CrawlStatus.Completed(crawlerOutput)) as CrawlResultat.Ferdig
    assertThat(updated.nettsider).containsExactlyElementsOf(crawlerOutput.map { URL(it.url) })
  }

  @DisplayName("når vi oppdaterer et resultat som ikke er ferdig, så skal framgangen oppdateres")
  @Test
  fun updateFramgang() {
    val ikkeFerdig =
        CrawlResultat.IkkeFerdig(
            URL("https://status.uri"),
            uutilsynetLoeysing,
            Instant.now(),
            CrawlResultat.Framgang(2, 100))
    val updated =
        updateStatus(ikkeFerdig, CrawlStatus.Running(CustomStatus(23, 100)))
            as CrawlResultat.IkkeFerdig
    assertThat(updated.framgang).isEqualTo(CrawlResultat.Framgang(23, 100))
  }
}
