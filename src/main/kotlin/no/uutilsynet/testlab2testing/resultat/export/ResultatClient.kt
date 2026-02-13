package no.uutilsynet.testlab2testing.resultat.export

import no.uutilsynet.testlab2testing.testresultat.model.TestresultatExport
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

@Component
class ResultatClient(
    restTemplate: RestTemplate,
    private val resultatRegisterProperties: ResultatRegisterProperties
) {

  val restClient = RestClient.create(restTemplate)

  fun putTestresultatList(testresultat: List<TestresultatExport>): Result<List<Long>> {
    return runCatching {
      restClient
          .post()
          .uri("${resultatRegisterProperties.host}/batch/testresultat")
          .body(testresultat)
          .retrieve()
          .body(object : ParameterizedTypeReference<List<Long>>() {})
          ?: throw NoSuchElementException(
              "Resultatregisteret returnerte null for liste av testresultatIder")
    }
  }
}

@ConfigurationProperties(prefix = "resultat")
data class ResultatRegisterProperties(val host: String)
