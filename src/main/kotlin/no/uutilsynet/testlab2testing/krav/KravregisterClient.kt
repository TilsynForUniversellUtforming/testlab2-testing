package no.uutilsynet.testlab2testing.krav

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate


@Service
class KravregisterClient(val restTemplate: RestTemplate, val properties: KravRegisterProperties) {

  private val logger = LoggerFactory.getLogger(KravregisterClient::class.java)

  @Cacheable("kravFromSuksesskriterium", unless = "#result==null")
  fun getKrav(suksesskriterium: String): KravWcag2x {
    logger.info(
        "Henter krav fra ${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium .")
    return restTemplate.getForObject(
        "${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium",
        KravWcag2x::class.java)
        ?: throw RuntimeException(
            "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
  }

  @Cacheable("kravFromId", unless = "#result==null")
  fun getWcagKrav(kravId: Int): KravWcag2x {
    return restTemplate.getForObject(
        "${properties.host}/v1/krav/wcag2krav/$kravId", KravWcag2x::class.java)
        ?: throw RuntimeException("Kravregisteret returnerte null for kravId $kravId")
  }

  @Cacheable("suksesskriteriumFromId", unless = "#result==null")
  fun getKravIdFromSuksesskritterium(suksesskriterium: String): Int {
    return getKrav(suksesskriterium).id
  }

  @Cacheable("suksesskriteriumFromKrav", unless = "#result == null")
  fun getSuksesskriteriumFromKrav(kravId: Int): String {
    return getWcagKrav(kravId).suksesskriterium
  }

  @Cacheable("kravList", unless = "#result==null")
  fun listKrav(): List<KravWcag2x> {
    return restTemplate.exchange(
        "${properties.host}/v1/krav/wcag2krav",
        org.springframework.http.HttpMethod.GET,
        null,
        object : ParameterizedTypeReference<List<KravWcag2x>>() {}
    ).body ?: throw RuntimeException("Kravregisteret returnerte null for liste av krav")
  }
}

@ConfigurationProperties(prefix = "kravregister")
data class KravRegisterProperties(val host: String)
