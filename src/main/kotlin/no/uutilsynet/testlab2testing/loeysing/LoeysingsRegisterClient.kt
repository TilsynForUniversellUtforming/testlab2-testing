package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "loeysingsregister")
data class LoeysingsRegisterProperties(val host: String)

@Component
class LoeysingsRegisterClient(
    val restTemplate: RestTemplate,
    val properties: LoeysingsRegisterProperties
) {
  val logger: Logger = LoggerFactory.getLogger(LoeysingsRegisterClient::class.java)

  fun saveLoeysing(id: Int, namn: String, url: URL, orgnummer: String): Result<Unit> = runCatching {
    restTemplate.postForLocation(
        "${properties.host}/v1/loeysing", Loeysing(id, namn, url, orgnummer))
    Unit
  }
}
