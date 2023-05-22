package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AutoTesterClientTest {
  private val objectMapper =
      jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @DisplayName("når responsen fra autotester er `Pending`, så skal det parses til responsklassen")
  @Test
  fun pending() {
    val jsonString = """{"runtimeStatus":"Pending", "output": null}"""
    val pending =
        objectMapper.readValue(
            jsonString, AutoTesterClient.AzureFunctionResponse.Pending::class.java)
    assertThat(pending).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Pending::class.java)
  }

  @DisplayName("når responsen fra autotester er `Running`, så skal det parses til responsklassen")
  @Test
  fun running() {
    val jsonString =
        """{"runtimeStatus":"Running", "output": null, "customStatus":{"testaSider":0, "talSider":0}}"""
    val running =
        objectMapper.readValue(
            jsonString, AutoTesterClient.AzureFunctionResponse.Running::class.java)
    assertThat(running).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Running::class.java)
  }

  @DisplayName("når responsen fra autotester er `Completed`, så skal det parses til responsklassen")
  @Test
  fun completed() {
    val jsonString =
        """{"runtimeStatus":"Completed", "output":[{
    "suksesskriterium": [
      "2.4.2"
    ],
    "side": "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
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
    "side": "https://www.uutilsynet.no/statistikk-og-rapporter/digitale-barrierar/1160",
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
  }]}"""
    val completed =
        objectMapper.readValue(
            jsonString, AutoTesterClient.AzureFunctionResponse.Completed::class.java)
    assertThat(completed).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Completed::class.java)
    assertThat(completed.output).hasSize(2)
  }

  @DisplayName("når responsen fra autotester er `Failed`, så skal det parses til responsklassen")
  @Test
  fun failed() {
    val jsonString = """{"runtimeStatus":"Failed", "output": "401 Unauthorized"}"""
    val failed =
        objectMapper.readValue(
            jsonString, AutoTesterClient.AzureFunctionResponse.Failed::class.java)
    assertThat(failed).isInstanceOf(AutoTesterClient.AzureFunctionResponse.Failed::class.java)
    assertThat(failed.output).isEqualTo("401 Unauthorized")
  }
}
