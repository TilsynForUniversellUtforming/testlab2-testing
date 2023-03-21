package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.net.URL
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "autotester")
data class AutoTesterProperties(val url: String, val code: String)

@Component
class AutoTesterClient(
    val restTemplate: RestTemplate,
    val autoTesterProperties: AutoTesterProperties
) {

  fun startTesting(maalingId: Int, crawlResultat: CrawlResultat.Ferdig): Result<URL> {
    return runCatching {
      val url = "${autoTesterProperties.url}?code=${autoTesterProperties.code}"
      val requestData =
          mapOf(
              "urls" to crawlResultat.nettsider,
              "idMaaling" to maalingId,
              "idLoeysing" to crawlResultat.loeysing.id)
      val statusUris = restTemplate.postForObject(url, requestData, StatusUris::class.java)
      statusUris?.statusQueryGetUri?.toURL()
          ?: throw RuntimeException("mangler statusQueryGetUri i responsen")
    }
  }

  data class StatusUris(val statusQueryGetUri: URI)

  data class AutoTesterResponse(
      val instanceId: String,
      val runtimeStatus: RuntimeStatus,
      val output: List<TestResultat>?
  )

  data class TestResultat(
      val _idSuksesskriterium: String,
      val _idTestregel: String,
      val _sideUtfall: String,
      val _brot: Boolean,
      val _samsvar: Boolean,
      val _ikkjeForekomst: Boolean,
      val _side: String,
      val _elementUtfall: String,
      val _element: ACTElement,
      val _idLoeysing: Int,
      val _idMaaling: Int
  )

  data class ACTElement(val htmlCode: String, val pointer: String, val accessibleName: String?)

  // Se
  // https://learn.microsoft.com/en-us/azure/azure-functions/durable/durable-functions-instance-management?tabs=csharp#query-instances
  // for alle statuser.
  enum class RuntimeStatus {
    Pending,
    Running,
    Completed
  }
}
