package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
                  restTemplate.getForObject(
                      testKoeyring.statusURL.toURI(), AzureFunctionResponse::class.java)!!
              TestKoeyring.updateStatus(testKoeyring, response)
            }
        is TestKoeyring.Starta ->
            runCatching {
              val response =
                  restTemplate.getForObject(
                      testKoeyring.statusURL.toURI(), AzureFunctionResponse::class.java)!!
              TestKoeyring.updateStatus(testKoeyring, response)
            }
        is TestKoeyring.Ferdig -> Result.success(testKoeyring)
        is TestKoeyring.Feila -> Result.success(testKoeyring)
      }

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "runtimeStatus")
  @JsonSubTypes(
      JsonSubTypes.Type(value = AzureFunctionResponse.Pending::class, name = "Pending"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Running::class, name = "Running"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Completed::class, name = "Completed"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Failed::class, name = "Failed"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Other::class, name = "ContinuedAsNew"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Other::class, name = "Terminated"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Other::class, name = "Suspended"))
  sealed class AzureFunctionResponse {
    object Pending : AzureFunctionResponse()
    object Running : AzureFunctionResponse()
    data class Completed(val output: List<TestResultat>) : AzureFunctionResponse()
    data class Failed(val output: String) : AzureFunctionResponse()
    data class Other(val output: String?) : AzureFunctionResponse()
  }

  data class StatusUris(val statusQueryGetUri: URI)
}
