package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.URI
import java.net.URL
import java.time.Duration.ofSeconds
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "autotester")
data class AutoTesterProperties(val url: String, val code: String)

@Component
class AutoTesterAdapter(
    restTemplateBuilder: RestTemplateBuilder,
    val autoTesterProperties: AutoTesterProperties
) {
  private val restTemplate = makeRestTemplate(restTemplateBuilder)

  private fun makeRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter()
    mappingJackson2HttpMessageConverter.objectMapper = objectMapper
    return restTemplateBuilder.messageConverters(mappingJackson2HttpMessageConverter).build()
  }

  fun runTests(urls: List<URL>): Result<AutoTesterResponse> {
    return runCatching {
      val requestData = mapOf("urls" to urls, "idMaaling" to 1, "idLoeysing" to 2)

      val url = "${autoTesterProperties.url}?code=${autoTesterProperties.code}"
      val statusUris = restTemplate.postForObject(url, requestData, StatusUris::class.java)
      if (statusUris?.statusQueryGetUri == null)
          throw RuntimeException("mangler statusQueryGetUri i responsen")

      waitForResponse(statusUris.statusQueryGetUri)
    }
  }

  private fun waitForResponse(statusQueryGetUri: URI): AutoTesterResponse {
    val response =
        restTemplate.getForObject(statusQueryGetUri, AutoTesterResponse::class.java)
            ?: throw RuntimeException("ingen data i responsen fra autotester")
    return when (response.runtimeStatus) {
      RuntimeStatus.Pending,
      RuntimeStatus.Running -> {
        Thread.sleep(ofSeconds(1).toMillis())
        waitForResponse(statusQueryGetUri)
      }
      RuntimeStatus.Completed -> {
        response
      }
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
