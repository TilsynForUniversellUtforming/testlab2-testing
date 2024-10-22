package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.time.Instant
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.crawlResultat
import no.uutilsynet.testlab2testing.forenkletkontroll.TestConstants.statusURL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TestKoeyringTest {

  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual =
        TestKoeyring.from(
            crawlResultat.loeysing,
            URI(statusURL).toURL(),
            Brukar("test", "testar"),
            crawlResultat.antallNettsider)
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
      "gitt ei testkøyring med tilstand `ferdig`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFerdig(response: AutoTesterClient.AutoTesterStatus, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.Ferdig(
            crawlResultat.loeysing,
            Instant.now(),
            URI(statusURL).toURL(),
            lenker = null,
            Brukar("test", "testar"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Ferdig::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `feila`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFeila(response: AutoTesterClient.AutoTesterStatus, tilstand: Class<*>) {
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
