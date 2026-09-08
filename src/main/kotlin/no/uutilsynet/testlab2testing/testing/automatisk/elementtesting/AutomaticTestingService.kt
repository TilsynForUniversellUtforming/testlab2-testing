package no.uutilsynet.testlab2testing.testing.automatisk.elementtesting

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AutomaticTestingService(val automaticTestingProperties: AutomaticTestingProperties) {

  fun testElement(autotestrerParams: AutotesterPayload): Result<AutotesterReports> {
    return runCatching {
      val response: AutotesterReports? =
          RestClient.create(automaticTestingProperties.url + "/autotest")
              .post()
              .body(autotestrerParams)
              .retrieve()
              .body(object : ParameterizedTypeReference<AutotesterResponse>() {})
              ?.reports
              ?.single()

      response ?: throw NoSuchElementException("No response from autotester")
    }
  }
}

data class AutotesterPayload(val htmlElement: String, val qualwebRule: String)

data class AutotesterResponse(val reports: List<AutotesterReports>)

data class AutotesterReports(val elementResultat: String, val elementUtfall: String)

@ConfigurationProperties(prefix = "autotester2")
data class AutomaticTestingProperties(val url: String)
