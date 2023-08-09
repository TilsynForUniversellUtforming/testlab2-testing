package no.uutilsynet.testlab2testing.maaling

import java.net.URI
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
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 2))
    val crawlerOutput =
        listOf(
            CrawlerOutput("https://www.uutilsynet.no/", "uutilsynet"),
            CrawlerOutput(
                "https://www.uutilsynet.no/veiledning/tilgjengelighetserklaering/1127",
                "uutilsynet"))
    val updated =
        updateStatus(ikkeFerdig, CrawlStatus.Completed(crawlerOutput)) as CrawlResultat.Ferdig
    assertThat(updated.nettsider)
        .containsExactlyElementsOf(crawlerOutput.map { URI(it.url).toURL() })
  }

  @DisplayName(
      "når vi oppdaterer et crawlresultat som ikke er ferdig, og output fra crawleren er tom, så skal crawlresultatet bli feilet")
  @Test
  fun toFeilet() {
    val ikkeFerdig =
        CrawlResultat.IkkeFerdig(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 2))
    val updated =
        updateStatus(ikkeFerdig, CrawlStatus.Completed(emptyList())) as CrawlResultat.Feilet
    assertThat(updated).isInstanceOf(CrawlResultat.Feilet::class.java)
  }

  @DisplayName(
      "når vi oppdaterer et crawlresultat, og ny status er `Terminated`, så skal crawlresultatet bli feilet")
  @Test
  fun terminated() {
    val ikkeFerdig =
        CrawlResultat.IkkeFerdig(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(1, 2))
    val updated = updateStatus(ikkeFerdig, CrawlStatus.Terminated)
    assertThat(updated).isInstanceOf(CrawlResultat.Feilet::class.java)
  }

  @DisplayName("når vi oppdaterer et resultat som ikke er ferdig, så skal framgangen oppdateres")
  @Test
  fun updateFramgang() {
    val ikkeFerdig =
        CrawlResultat.IkkeFerdig(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 100))
    val updated =
        updateStatus(ikkeFerdig, CrawlStatus.Running(CustomStatus(23, 100)))
            as CrawlResultat.IkkeFerdig
    assertThat(updated.framgang).isEqualTo(Framgang(23, 100))
  }
}
