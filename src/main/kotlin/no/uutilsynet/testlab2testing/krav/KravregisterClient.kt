package no.uutilsynet.testlab2testing.krav

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class KravregisterClient(val restTemplate: RestTemplate, val properties: KravRegisterProperties) {

  fun getKrav(suksesskriterium: String): Result<Krav> {
    return runCatching {
      restTemplate.getForObject(
          "${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium",
          Krav::class.java)
          ?: throw RuntimeException(
              "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
    }
  }

  fun getWcagKrav(kravId: Int): Result<KravWcag2x> {
    return runCatching {
      restTemplate.getForObject(
          "${properties.host}/v1/krav/wcag2krav/$kravId", KravWcag2x::class.java)
          ?: throw RuntimeException("Kravregisteret returnerte null for kravId $kravId")
    }
  }
}

@ConfigurationProperties(prefix = "kravregister")
data class KravRegisterProperties(val host: String)
