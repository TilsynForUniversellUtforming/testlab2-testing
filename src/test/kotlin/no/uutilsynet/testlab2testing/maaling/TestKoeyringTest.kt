package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URL
import java.time.Instant
import java.util.stream.Stream
import no.uutilsynet.testlab2testing.maaling.TestConstants.uutilsynetLoeysing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class TestKoeyringTest {
  private val crawlResultat =
      CrawlResultat.Ferdig(
          listOf(
              URL("https://www.uutilsynet.no/"),
              URL("https://www.uutilsynet.no/underside/1"),
              URL("https://www.uutilsynet.no/underside/2")),
          URL("https://status.url"),
          uutilsynetLoeysing,
          Instant.now())
  @Test
  @DisplayName("ei ny TestKøyring startar med status `ikkje starta`")
  fun nyTestKoeyring() {
    val actual = TestKoeyring.from(crawlResultat, URL("https://status.url"))
    assertThat(actual).isInstanceOf(TestKoeyring.IkkjeStarta::class.java)
    assertThat((actual as TestKoeyring.IkkjeStarta).statusURL.toString())
        .isEqualTo("https://status.url")
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `ikkje starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatus(response: AutoTesterClient.AzureFunctionResponse, tilstand: Class<*>) {
    val testKoeyring =
        TestKoeyring.IkkjeStarta(crawlResultat, Instant.now(), URL("http://status.url"))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  @DisplayName(
      "gitt ei testkøyring med tilstand `starta`, test riktig kombinasjon av respons og ny tilstand")
  fun testUpdateStatusFromStarta(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring =
        TestKoeyring.Starta(
            crawlResultat,
            Instant.now(),
            URL("http://status.url"),
            TestKoeyring.Framgang(0, crawlResultat.nettsider.size))
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(tilstand)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `ferdig`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFerdig(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring =
        TestKoeyring.Ferdig(
            crawlResultat, Instant.now(), URL("https://status.url"), testResultater())
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Ferdig::class.java)
  }

  @DisplayName(
      "gitt ei testkøyring med tilstand `feila`, så blir ikkje tilstanden endra uansett kva ny tilstand som blir rapportert")
  @ParameterizedTest
  @MethodSource("pairsOfResponseTilstand")
  fun testUpdateStatusFromFeila(
      response: AutoTesterClient.AzureFunctionResponse,
      tilstand: Class<*>
  ) {
    val testKoeyring = TestKoeyring.Feila(crawlResultat, Instant.now(), "dette går ikkje")
    val actual = TestKoeyring.updateStatus(testKoeyring, response)
    assertThat(actual).isInstanceOf(TestKoeyring.Feila::class.java)
  }

  @Nested
  @DisplayName("aggregering på testregel")
  inner class AggregeringPaaTestregel {
    @DisplayName("gitt et testresultat for UUTilsynet")
    @Nested
    inner class TestResultatForUUTilsynet {
      private val result = TestKoeyring.aggregerPaaTestregel(testKoeyringar(), 46)

      @Test
      @DisplayName("så skal aggregeringen gi oss en liste med et element per testregel")
      fun ettElementPerTestregel() {
        assertThat(result.map { it.testregelId })
            .containsExactly(
                "QW-ACT-R1", "QW-ACT-R2", "QW-ACT-R5", "QW-ACT-R11", "QW-ACT-R28", "QW-ACT-R37")
      }

      @Test
      @DisplayName("så skal hvert element i aggregeringen inneholde id-en på målingen")
      fun maalingId() {
        result.forEach { assertThat(it.maalingId).isEqualTo(46) }
      }

      @Test
      @DisplayName("så skal hvert element i aggregeringen inneholde id-en på løsningen")
      fun loeysingId() {
        result.forEach { assertThat(it.loeysing.id).isEqualTo(1) }
      }

      @Test
      @DisplayName("så skal hvert element i aggregeringen inneholde navnet på løsningen")
      fun loeysingNavn() {
        result.forEach { assertThat(it.loeysing.namn).isEqualTo("UUTilsynet") }
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 1",
                  "QW-ACT-R2 1",
                  "QW-ACT-R5 1",
                  "QW-ACT-R11 2",
                  "QW-ACT-R28 1",
                  "QW-ACT-R37 0"])
      @DisplayName("så skal antall tester med samsvar være gitt for hver aggregering")
      fun antallTesterMedSamsvar(s: String) {
        val (testregelId, antallSamsvar) = s.split(" ")
        result
            .find { it.testregelId == testregelId }
            ?.let { assertThat(it.talElementSamsvar).isEqualTo(antallSamsvar.toInt()) }
            ?: throw AssertionError("Fant ikke aggregering for testregel $testregelId")
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 0",
                  "QW-ACT-R2 0",
                  "QW-ACT-R5 0",
                  "QW-ACT-R11 0",
                  "QW-ACT-R28 1",
                  "QW-ACT-R37 0"])
      @DisplayName("så skal antall tester med brot være gitt for hver aggregering")
      fun antallTesterMedBrot(s: String) {
        val (testregelId, antallBrot) = s.split(" ")
        result
            .find { it.testregelId == testregelId }
            ?.let { assertThat(it.talElementBrot).isEqualTo(antallBrot.toInt()) }
            ?: throw AssertionError("Fant ikke aggregering for testregel $testregelId")
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 0",
                  "QW-ACT-R2 0",
                  "QW-ACT-R5 0",
                  "QW-ACT-R11 0",
                  "QW-ACT-R28 0",
                  "QW-ACT-R37 2"])
      @DisplayName("så skal antall tester med varsel være gitt for hver aggregering")
      fun antallTesterMedVarsel(s: String) {
        val (testregelId, antallVarsel) = s.split(" ")
        result
            .find { it.testregelId == testregelId }
            ?.let { assertThat(it.talElementVarsel).isEqualTo(antallVarsel.toInt()) }
            ?: throw AssertionError("Fant ikke aggregering for testregel $testregelId")
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 1",
                  "QW-ACT-R2 1",
                  "QW-ACT-R5 1",
                  "QW-ACT-R11 2",
                  "QW-ACT-R28 0",
                  "QW-ACT-R37 0"])
      @DisplayName("så skal antall sider med samsvar være gitt for hver aggregering")
      fun antallSiderMedSamsvar(s: String) {
        val (testregelId, antallSamsvar) = s.split(" ")
        assertThat(result.find { it.testregelId == testregelId }?.talSiderSamsvar)
            .isEqualTo(antallSamsvar.toInt())
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 0",
                  "QW-ACT-R2 0",
                  "QW-ACT-R5 0",
                  "QW-ACT-R11 0",
                  "QW-ACT-R28 1",
                  "QW-ACT-R37 0"])
      @DisplayName("så skal antall sider med samsvar være gitt for hver aggregering")
      fun antallSiderMedBrot(s: String) {
        val (testregelId, antallBrot) = s.split(" ")
        assertThat(result.find { it.testregelId == testregelId }?.talSiderBrot)
            .isEqualTo(antallBrot.toInt())
      }

      @ParameterizedTest
      @ValueSource(
          strings =
              [
                  "QW-ACT-R1 2",
                  "QW-ACT-R2 2",
                  "QW-ACT-R5 2",
                  "QW-ACT-R11 1",
                  "QW-ACT-R28 2",
                  "QW-ACT-R37 2"])
      @DisplayName("så skal antall sider uten forekomst være gitt for hver aggregering")
      fun antallSiderUtenForekomst(s: String) {
        val (testregelId, antallSiderUtenForekomst) = s.split(" ")
        assertThat(result.find { it.testregelId == testregelId }?.talSiderIkkjeForekomst)
            .isEqualTo(antallSiderUtenForekomst.toInt())
      }

      @Test
      @DisplayName(
          "så skal hver aggregering ha et felt for det første suksesskriteriet, og et felt for resten")
      fun toFeltForSuksesskriterier() {
        val testCases =
            listOf(
                Triple("QW-ACT-R1", "2.4.2", emptyList()),
                Triple("QW-ACT-R2", "3.1.1", emptyList()),
                Triple("QW-ACT-R5", "3.1.1", emptyList()),
                Triple("QW-ACT-R11", "4.1.2", emptyList()),
                Triple("QW-ACT-R28", "4.1.2", emptyList()),
                Triple("QW-ACT-R37", "1.4.3", listOf("1.4.6")))

        testCases.forEach { (testregelId, suksesskriterium, suksesskriterier) ->
          val aggregeringR1 = result.find { it.testregelId == testregelId }
          assertThat(aggregeringR1?.suksesskriterium).isEqualTo(suksesskriterium)
          assertThat(aggregeringR1?.fleireSuksesskriterium).isEqualTo(suksesskriterier.toList())
        }
      }
    }

    private fun testKoeyringar(): List<TestKoeyring.Ferdig> {
      val testResultat =
          jacksonObjectMapper()
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .readValue(testResultatJson, object : TypeReference<List<TestResultat>>() {})
      val nettsider = testResultat.map { it.side }.distinctBy { it.toString() }
      val crawlResultat =
          CrawlResultat.Ferdig(
              nettsider, URL("https://status.url"), uutilsynetLoeysing, Instant.now())
      return listOf(
          TestKoeyring.Ferdig(
              crawlResultat, Instant.now(), URL("https://status.url"), testResultat))
    }

    private val testResultatJson =
        """
      [
        {
          "suksesskriterium": [
            "2.4.2"
          ],
          "side": "https://www.uutilsynet.no/side1",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R1",
          "sideNivaa": 1,
          "testVartUtfoert": "3/23/2023, 11:15:54 AM",
          "elementUtfall": "The `title` element exists and it's not empty ('').",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html > head:nth-child(1) > title:nth-child(18)",
              "htmlCode": "PHRpdGxlPkRpZ2l0YWxlIGJhcnJpZXJhciB8IFRpbHN5bmV0IGZvciB1bml2ZXJzZWxsIHV0Zm9ybWluZyBhdiBpa3Q8L3RpdGxlPg=="
            }
          ]
        },
        {
          "suksesskriterium": [
            "3.1.1"
          ],
          "side": "https://www.uutilsynet.no/side1",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R2",
          "sideNivaa": 1,
          "testVartUtfoert": "3/23/2023, 11:15:54 AM",
          "elementUtfall": "The `lang` attribute exists and has a value.",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html",
              "htmlCode": "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT48L2JvZHk+PC9odA=="
            }
          ]
        },
        {
          "suksesskriterium": [
            "3.1.1"
          ],
          "side": "https://www.uutilsynet.no/side2",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R5",
          "sideNivaa": 1,
          "testVartUtfoert": "3/23/2023, 11:15:54 AM",
          "elementUtfall": "The `lang` attribute has a valid value.",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html",
              "htmlCode": "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT53aW5kb3cuZGF0YQ=="
            }
          ]
        },
        {
          "suksesskriterium": [
            "4.1.2"
          ],
          "side": "https://www.uutilsynet.no/side2",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R11",
          "sideNivaa": 1,
          "testVartUtfoert": "3/23/2023, 11:15:54 AM",
          "elementUtfall": "The test target has an accessible name.",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > div:nth-child(2) > div:nth-child(1) > header:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > button:nth-child(1)",
              "htmlCode": "PGJ1dHRvbiBjbGFzcz0iaGVhZGVyLWJ1dHRvbiBoZWFkZXItYnV0dG9uLS1zZWFyY2ggY29sbGFwc2VkIiBkYXRhLWJzLXRvZ2dsZT0iY29sbGFwc2UiIGRhdGEtYnMtdGFyZw=="
            }
          ]
        },
        {
          "suksesskriterium": [
            "4.1.2"
          ],
          "side": "https://www.uutilsynet.no/side3",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R11",
          "sideNivaa": 1,
          "testVartUtfoert": "3/23/2023, 11:15:54 AM",
          "elementUtfall": "The test target has an accessible name.",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > div:nth-child(2) > div:nth-child(1) > header:nth-child(1) > div:nth-child(3) > div:nth-child(1) > nav:nth-child(1) > div:nth-child(1) > button:nth-child(1)",
              "htmlCode": "PGJ1dHRvbiBpZD0ic3VibWVudS0tYnV0dG9uLS1pbnRyby10aWwtdXUiIGNsYXNzPSJzdWJtZW51X190b2dnbGUgdGV4dC1ib2R5LTMwMCBkcm9wZG93bi10b2dnbGUiIGRhdA=="
            }
          ]
        },
        {
          "suksesskriterium": ["4.1.2"],
          "side": "https://www.uutilsynet.no/side3",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R28",
          "sideNivaa": 0,
          "testVartUtfoert": "4/19/2023, 11:37:55 AM",
          "elementUtfall": "The test target `role` doesn't have required state or property",
          "elementResultat": "samsvar",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > div:nth-child(3) > div:nth-child(3) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div:nth-child(1)",
              "htmlCode": "PGRpdiBpZD0iaW8tZmlsdGVyLXBhbmVsIiByb2xlPSJkaWFsb2ciIGFyaWEtbGFiZWxsZWRieT0iaW8tZmlsdGVyLXBhbmVsLWhlYWRpbmciIGNsYXNzPSJpby1maWx0ZXItcA=="
            }
          ]
        },
        {
          "suksesskriterium": ["4.1.2"],
          "side": "https://www.uutilsynet.no/side3",
          "maalingId": 46,
          "loeysingId": 1,
          "testregelId": "QW-ACT-R28",
          "sideNivaa": 0,
          "testVartUtfoert": "4/19/2023, 11:37:55 AM",
          "elementUtfall": "The test target has unlisted required states or properties.",
          "elementResultat": "brot",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > div:nth-child(3) > div:nth-child(3) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > label:nth-child(1) > input:nth-child(1)",
              "htmlCode": "PGlucHV0IGFyaWEtZXhwYW5kZWQ9ImZhbHNlIiB0eXBlPSJzZWFyY2giIGF1dG9jb21wbGV0ZT0ib2ZmIiBwbGFjZWhvbGRlcj0iVmVpYWRyZXNzZSIgYXJpYS1oYXNwb3B1cA=="
            }
          ]
        },
        {
          "suksesskriterium": ["1.4.3", "1.4.6"],
          "side": "https://www.uutilsynet.no/side3",
          "maalingId": 63,
          "loeysingId": 4,
          "testregelId": "QW-ACT-R37",
          "sideNivaa": 0,
          "testVartUtfoert": "4/21/2023, 8:17:26 AM",
          "elementUtfall": "Element has an image on background.",
          "elementResultat": "varsel",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > main:nth-child(4) > section:nth-child(6) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > a:nth-child(1) > div:nth-child(1)",
              "htmlCode": "PGRpdiBjbGFzcz0iaW1hZ2UtdGl0bGUgd2hpdGUtdGl0bGUiPkJpdGNvaW4gVHV0b3JpYWw8L2Rpdj4="
            }
          ]
        },
        {
          "suksesskriterium": ["1.4.3", "1.4.6"],
          "side": "https://www.uutilsynet.no/side3",
          "maalingId": 63,
          "loeysingId": 4,
          "testregelId": "QW-ACT-R37",
          "sideNivaa": 0,
          "testVartUtfoert": "4/21/2023, 8:17:26 AM",
          "elementUtfall": "Element has an image on background.",
          "elementResultat": "varsel",
          "elementOmtale": [
            {
              "pointer": "html > body:nth-child(2) > main:nth-child(4) > section:nth-child(6) > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(3) > div:nth-child(1) > a:nth-child(1) > div:nth-child(1)",
              "htmlCode": "PGRpdiBjbGFzcz0iaW1hZ2UtdGl0bGUgd2hpdGUtdGl0bGUiPkJsb2NrY2hhaW4gVHV0b3JpYWw8L2Rpdj4="
            }
          ]
        }
      ]
    """
            .trimIndent()
  }

  companion object {
    @JvmStatic
    fun pairsOfResponseTilstand(): Stream<Arguments> {
      return Stream.of(
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Pending, TestKoeyring.IkkjeStarta::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Running(AutoTesterClient.CustomStatus(0, 1)),
              TestKoeyring.Starta::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Completed(testResultater()),
              TestKoeyring.Ferdig::class.java),
          Arguments.of(
              AutoTesterClient.AzureFunctionResponse.Failed("401 Unauthorized"),
              TestKoeyring.Feila::class.java))
    }

    fun testResultater() =
        listOf(
            TestResultat(
                listOf("3.1.1"),
                URL("https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160"),
                "QW-ACT-R5",
                1,
                TestResultat.parseLocalDateTime("3/23/2023, 11:15:54 AM"),
                "The `lang` attribute has a valid value.",
                "samsvar",
                TestResultat.ACTElement(
                    "html",
                    "PGh0bWwgbGFuZz0ibm4iIGRpcj0ibHRyIiBwcmVmaXg9Im9nOiBodHRwczovL29ncC5tZS9ucyMiIGNsYXNzPSIganMiPjxoZWFkPjwvaGVhZD48Ym9keT53aW5kb3cuZGF0YQ==")),
            TestResultat(
                listOf("4.1.2"),
                URL("https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160"),
                "QW-ACT-R11",
                1,
                TestResultat.parseLocalDateTime("3/23/2023, 11:15:54 AM"),
                "The test target has an accessible name.",
                "samsvar",
                TestResultat.ACTElement(
                    "html > body:nth-child(2) > div:nth-child(2) > div:nth-child(1) > header:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(2) > button:nth-child(1)",
                    "PGJ1dHRvbiBjbGFzcz0iaGVhZGVyLWJ1dHRvbiBoZWFkZXItYnV0dG9uLS1zZWFyY2ggY29sbGFwc2VkIiBkYXRhLWJzLXRvZ2dsZT0iY29sbGFwc2UiIGRhdGEtYnMtdGFyZw==")))
  }
}
