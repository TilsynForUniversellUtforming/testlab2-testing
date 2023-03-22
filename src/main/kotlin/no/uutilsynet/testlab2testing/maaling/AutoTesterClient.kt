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

  fun updateStatus(testKoeyring: TestKoeyring): Result<TestKoeyring> =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta ->
            runCatching {
              val response =
                  restTemplate.getForObject(testKoeyring.statusURL.toURI(), Response::class.java)!!
              TestKoeyring.updateStatus(testKoeyring, response)
            }
        is TestKoeyring.Starta ->
            runCatching {
              val response =
                  restTemplate.getForObject(testKoeyring.statusURL.toURI(), Response::class.java)!!
              TestKoeyring.updateStatus(testKoeyring, response)
            }
        is TestKoeyring.Feila -> Result.success(testKoeyring)
      }

  data class StatusUris(val statusQueryGetUri: URI)

  data class Response(
      val runtimeStatus: RuntimeStatus,
  )

  enum class RuntimeStatus {
    Pending,
    Running,
    Completed,
    ContinuedAsNew,
    Failed,
    Terminated,
    Suspended
  }

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
}
