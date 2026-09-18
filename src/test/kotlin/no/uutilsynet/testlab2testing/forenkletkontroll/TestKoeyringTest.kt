package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.crawlResultat
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.statusURL
import no.uutilsynet.testlab2testing.testing.automatisk.AutoTesterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestKoeyring
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TestKoeyringTest {

  @Test
  fun `konverterer TestkoeyringDTO IkkjeStarta til TestKoeyring IkkjeStarta`() {
    val brukar = Brukar("test", "testar")
    val sistOppdatert = Instant.now()
    val dto =
        TestkoeyringDTO.IkkjeStarta(
            maalingId = 1,
            loeysingId = crawlResultat.loeysing.id,
            brukarId = 1,
            lenkerTesta = 0,
            sistOppdatert = sistOppdatert,
            statusURL = URI(statusURL).toURL())

    val actual =
        TestKoeyring.from(dto, crawlResultat.loeysing, brukar, crawlResultat.antallNettsider)

    assertThat(actual)
        .isEqualTo(
            TestKoeyring.IkkjeStarta(
                loeysing = crawlResultat.loeysing,
                sistOppdatert = sistOppdatert,
                statusURL = URI(statusURL).toURL(),
                brukar = brukar,
                antallNettsider = crawlResultat.antallNettsider))
  }

  @Test
  fun `konverterer TestkoeyringDTO Starta til TestKoeyring Starta`() {
    val brukar = Brukar("test", "testar")
    val sistOppdatert = Instant.now()
    val dto =
        TestkoeyringDTO.Starta(
            maalingId = 1,
            loeysingId = crawlResultat.loeysing.id,
            brukarId = 1,
            lenkerTesta = 2,
            sistOppdatert = sistOppdatert,
            statusURL = URI(statusURL).toURL())

    val actual =
        TestKoeyring.from(dto, crawlResultat.loeysing, brukar, crawlResultat.antallNettsider)

    assertThat(actual)
        .isEqualTo(
            TestKoeyring.Starta(
                loeysing = crawlResultat.loeysing,
                sistOppdatert = sistOppdatert,
                statusURL = URI(statusURL).toURL(),
                framgang = Framgang(2, crawlResultat.antallNettsider),
                brukar = brukar,
                antallNettsider = crawlResultat.antallNettsider))
  }

  @Test
  fun `konverterer TestkoeyringDTO Ferdig til TestKoeyring Ferdig`() {
    val brukar = Brukar("test", "testar")
    val sistOppdatert = Instant.now()
    val lenker =
        AutoTesterClient.AutoTesterLenker(
            URI("https://fullt.resultat").toURL(),
            URI("https://brot.resultat").toURL(),
            URI("https://aggregering.resultat").toURL(),
            URI("https://aggregeringSK.resultat").toURL(),
            URI("https://aggregeringSide.resultat").toURL(),
            URI("https://aggregeringSideTR.resultat").toURL(),
            URI("https://aggregeringLoeysing.resultat").toURL(),
        )
    val dto =
        TestkoeyringDTO.Ferdig(
            maalingId = 1,
            loeysingId = crawlResultat.loeysing.id,
            brukarId = 1,
            lenkerTesta = crawlResultat.antallNettsider,
            sistOppdatert = sistOppdatert,
            statusURL = URI(statusURL).toURL(),
            lenker = lenker)

    val actual =
        TestKoeyring.from(dto, crawlResultat.loeysing, brukar, crawlResultat.antallNettsider)

    assertThat(actual)
        .isEqualTo(
            TestKoeyring.Ferdig(
                loeysing = crawlResultat.loeysing,
                sistOppdatert = sistOppdatert,
                statusURL = URI(statusURL).toURL(),
                lenker = lenker,
                brukar = brukar,
                antallNettsider = crawlResultat.antallNettsider))
  }

  @Test
  fun `konverterer TestkoeyringDTO Feila til TestKoeyring Feila`() {
    val brukar = Brukar("test", "testar")
    val sistOppdatert = Instant.now()
    val dto =
        TestkoeyringDTO.Feila(
            maalingId = 1,
            loeysingId = crawlResultat.loeysing.id,
            brukarId = 1,
            lenkerTesta = null,
            sistOppdatert = sistOppdatert,
            feilmelding = "Feila")

    val actual =
        TestKoeyring.from(dto, crawlResultat.loeysing, brukar, crawlResultat.antallNettsider)

    assertThat(actual)
        .isEqualTo(
            TestKoeyring.Feila(
                loeysing = crawlResultat.loeysing,
                sistOppdatert = sistOppdatert,
                feilmelding = "Feila",
                brukar = brukar))
  }

  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual = TestKoeyring.from(crawlResultat, URI(statusURL).toURL(), Brukar("test", "testar"))
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
    assertThat(actual.statusURL.toString()).isEqualTo(statusURL)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `ikkje starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatus(response: AutoTesterClient.AutoTesterStatus, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.IkkjeStarta(
            crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            Brukar("test", "testar"),
            crawlResultat.antallNettsider)
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatusFromStarta(response: AutoTesterClient.AutoTesterStatus, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            Framgang(0, crawlResultat.nettsider.size),
            Brukar("test", "testar"),
            crawlResultat.antallNettsider)
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @DisplayName("når ny responsen er `Terminated`, så skal ny tilstand bli `feila`")
  @Test
  fun testUpdateStatusFromStartaTerminated() {
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            Framgang(0, crawlResultat.nettsider.size),
            Brukar("test", "testar"),
            crawlResultat.antallNettsider)
    val actual =
        TestKoeyring.updateStatus(testKoeyring, AutoTesterClient.AutoTesterStatus.Terminated)
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `ferdig`, så blir ikkje tilstanden endra " +
          "uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFerdig(response: AutoTesterClient.AutoTesterStatus) {
    val testKoeyring =
        TestKoeyring.Ferdig(
            crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            lenker = null,
            Brukar("test", "testar"),
            crawlResultat.antallNettsider)
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Ferdig::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `feila`, så blir ikkje tilstanden endra " +
          "uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFeila(response: AutoTesterClient.AutoTesterStatus) {
    val testKoeyring =
        TestKoeyring.Feila(
            crawlResultat.loeysing, Instant.now(), "dette går ikkje", Brukar("test", "testar"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  companion object {
    @JvmStatic
    fun pairsOfResponseTilstand(): Stream<Arguments> {
      return Stream.of(
          Arguments.of(
              AutoTesterClient.AutoTesterStatus.Pending, TestKoeyring.IkkjeStarta::class.java),
          Arguments.of(
              AutoTesterClient.AutoTesterStatus.Running(AutoTesterClient.CustomStatus(0, 1)),
              TestKoeyring.Starta::class.java),
          Arguments.of(
              AutoTesterClient.AutoTesterStatus.Completed(
                  AutoTesterClient.AutoTesterLenker(
                      URI("https://fullt.resultat").toURL(),
                      URI("https://brot.resultat").toURL(),
                      URI("https://aggregering.resultat").toURL(),
                      URI("https://aggregeringSK.resultat").toURL(),
                      URI("https://aggregeringSide.resultat").toURL(),
                      URI("https://aggregeringSideTR.resultat").toURL(),
                      URI("https://aggregeringLoeysing.resultat").toURL(),
                  )),
              TestKoeyring.Ferdig::class.java),
          Arguments.of(
              AutoTesterClient.AutoTesterStatus.Failed("401 Unauthorized"),
              TestKoeyring.Feila::class.java))
    }
  }
}
