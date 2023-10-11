package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.time.Instant
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("tester for CrawlResultat")
class CrawlResultatKtTest {

  @DisplayName(
      "når vi får status Running fra crawler skal vi oppdatere fra ikkje starta til starta")
  @Test
  fun toStarta() {
    val ikkjeStarta =
        CrawlResultat.IkkjeStarta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now())
    val updated = updateStatus(ikkjeStarta, CrawlStatus.Running(CustomStatus(23, 100)))
    assertThat(updated).isInstanceOf(CrawlResultat.Starta::class.java)
  }

  @DisplayName("når vi får status Pending fra crawler skal status ikkje starta stå")
  @Test
  fun updatePending() {
    val ikkjeStarta =
        CrawlResultat.IkkjeStarta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now())
    val updated = updateStatus(ikkjeStarta, CrawlStatus.Pending)
    assertThat(updated).isInstanceOf(CrawlResultat.IkkjeStarta::class.java)
  }

  @DisplayName(
      "når vi oppdaterer fra ikke ferdig til ferdig, så skal crawlresultatet ha en liste med nettsider")
  @Test
  fun toFerdig() {
    val starta =
        CrawlResultat.Starta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 2))
    val crawlerOutput =
        listOf(
            CrawlerOutput("https://www.uutilsynet.no/", "uutilsynet"),
            CrawlerOutput(
                "https://www.uutilsynet.no/veiledning/tilgjengelighetserklaering/1127",
                "uutilsynet"))
    val updated = updateStatus(starta, CrawlStatus.Completed(crawlerOutput))
    assertThat(updated).isInstanceOf(CrawlResultat.Ferdig::class.java)

    val ferdig = updated as CrawlResultat.Ferdig
    assertThat(ferdig.nettsider)
        .containsExactlyElementsOf(crawlerOutput.map { URI(it.url).toURL() })
    assertThat(ferdig.antallNettsider).isEqualTo(crawlerOutput.size)
  }

  @DisplayName(
      "når vi oppdaterer et crawlresultat som ikke er ferdig, og output fra crawleren er tom, så skal crawlresultatet bli feilet")
  @Test
  fun toFeilet() {
    val starta =
        CrawlResultat.Starta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 2))
    val updated = updateStatus(starta, CrawlStatus.Completed(emptyList()))
    assertThat(updated).isInstanceOf(CrawlResultat.Feila::class.java)
  }

  @DisplayName(
      "når vi oppdaterer et crawlresultat, og ny status er `Terminated`, så skal crawlresultatet bli feilet")
  @Test
  fun terminated() {
    val starta =
        CrawlResultat.Starta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(1, 2))
    val updated = updateStatus(starta, CrawlStatus.Terminated)
    assertThat(updated).isInstanceOf(CrawlResultat.Feila::class.java)
  }

  @DisplayName("når vi oppdaterer et resultat som ikke er ferdig, så skal framgangen oppdateres")
  @Test
  fun updateFramgang() {
    val starta =
        CrawlResultat.Starta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 100))
    val updated = updateStatus(starta, CrawlStatus.Running(CustomStatus(23, 100)))
    assertThat(updated).isInstanceOf(CrawlResultat.Starta::class.java)
    assertThat((updated as CrawlResultat.Starta).framgang).isEqualTo(Framgang(23, 100))
  }

  @DisplayName(
      "når vi oppdaterer et resultat fra ikke ferdig til ferdig, og crawlresultatet inneholder en ugyldig url, så skal denne url-en fjernes, og resultatet settes til ferdig")
  @Test
  fun toFerdigWithInvalidUrl() {
    val starta =
        CrawlResultat.Starta(
            URI("https://status.uri").toURL(), uutilsynetLoeysing, Instant.now(), Framgang(2, 2))
    // output med en gyldig og en ugyldig url
    val crawlerOutput =
        listOf(
            CrawlerOutput("https://www.uutilsynet.no/", "gyldig_url"),
            CrawlerOutput("https://www.uutilsynet.no/[VIEWURL]", "ugyldig_url"))
    val updated = updateStatus(starta, CrawlStatus.Completed(crawlerOutput))
    assertThat(updated).isInstanceOf(CrawlResultat.Ferdig::class.java)
    assertThat((updated as CrawlResultat.Ferdig).nettsider)
        .containsExactlyElementsOf(listOf(crawlerOutput[0].url).map { URI(it).toURL() })
  }
}
