package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.net.URI
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
              "idLoeysing" to crawlResultat.loeysing.id,
              "resultatSomFil" to true)
      val statusUris = restTemplate.postForObject(url, requestData, StatusUris::class.java)
      statusUris?.statusQueryGetUri?.toURL()
          ?: throw RuntimeException("mangler statusQueryGetUri i responsen")
    }
  }

  fun updateStatus(testKoeyring: TestKoeyring): Result<TestKoeyring> =
      when (testKoeyring) {
        is TestKoeyring.Ferdig -> Result.success(testKoeyring)
        is TestKoeyring.Feila -> Result.success(testKoeyring)
        is TestKoeyring.IkkjeStarta,
        is TestKoeyring.Starta -> {
          val statusURL =
              runCatching {
                    when (testKoeyring) {
                      is TestKoeyring.IkkjeStarta -> testKoeyring.statusURL
                      is TestKoeyring.Starta -> testKoeyring.statusURL
                      else -> throw RuntimeException("Ugyldig tilstand")
                    }
                  }
                  .getOrElse {
                    return Result.failure(it)
                  }

          val response =
              runCatching {
                    restTemplate.getForObject(
                        statusURL.toURI(), AzureFunctionResponse::class.java)!!
                  }
                  .getOrElse {
                    return Result.failure(it)
                  }

          Result.success(TestKoeyring.updateStatus(testKoeyring, response))
        }
      }

  fun fetchFulltResultat(testKoeyring: TestKoeyring.Ferdig): Result<List<TestResultat>> =
      testKoeyring.lenker?.let { lenker ->
        runCatching {
          restTemplate
              .getForObject(lenker.urlFulltResultat.toURI(), Array<Array<TestResultat>>::class.java)
              ?.flatten()
              ?.toList()
              ?: throw RuntimeException(
                  "Vi fikk ingen resultater da vi forsøkte å hente testresultater fra ${lenker.urlFulltResultat}")
        }
      }
          ?: Result.success(testKoeyring.testResultat)

  suspend fun fetchFulltResultat(
      testKoeyringar: List<TestKoeyring.Ferdig>
  ): List<Result<List<TestResultat>>> = coroutineScope {
    val deferreds =
        testKoeyringar.map { testKoeyring -> async { fetchFulltResultat(testKoeyring) } }
    deferreds.awaitAll()
  }

  data class CustomStatus(val testaSider: Int, val talSider: Int)

  @JsonDeserialize(using = AutoTesterOutputDeserializer::class)
  sealed class AutoTesterOutput {
    data class TestResultater(val testResultater: List<TestResultat>) : AutoTesterOutput()
    data class Lenker(val urlFulltResultat: URL, val urlBrot: URL) : AutoTesterOutput()
  }

  class AutoTesterOutputDeserializer : JsonDeserializer<AutoTesterOutput>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): AutoTesterOutput {
      val node = jp.readValueAsTree<JsonNode>()
      return when {
        node.isArray ->
            AutoTesterOutput.TestResultater(
                node.map { objectNode ->
                  ctxt.readTreeAsValue(objectNode, TestResultat::class.java)
                })
        node.has("urlFulltResultat") ->
            AutoTesterOutput.Lenker(
                URL(node["urlFulltResultat"].asText()), URL(node["urlBrot"].asText()))
        else -> throw RuntimeException("Ukjent output fra AutoTester")
      }
    }
  }

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "runtimeStatus")
  @JsonSubTypes(
      JsonSubTypes.Type(value = AzureFunctionResponse.Pending::class, name = "Pending"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Running::class, name = "Running"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Completed::class, name = "Completed"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Failed::class, name = "Failed"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Terminated::class, name = "Terminated"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Other::class, name = "ContinuedAsNew"),
      JsonSubTypes.Type(value = AzureFunctionResponse.Other::class, name = "Suspended"))
  sealed class AzureFunctionResponse {
    object Pending : AzureFunctionResponse()
    data class Running(val customStatus: CustomStatus?) : AzureFunctionResponse()
    data class Completed(val output: AutoTesterOutput) : AzureFunctionResponse()
    data class Failed(val output: String) : AzureFunctionResponse()
    object Terminated : AzureFunctionResponse()
    data class Other(val output: String?) : AzureFunctionResponse()
  }

  data class StatusUris(val statusQueryGetUri: URI)
}
