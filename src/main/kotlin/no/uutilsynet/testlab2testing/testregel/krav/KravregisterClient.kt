package no.uutilsynet.testlab2testing.testregel.krav

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class KravregisterClient(val restClient: RestClient, val properties: KravRegisterProperties) {

  private val logger = LoggerFactory.getLogger(KravregisterClient::class.java)

  private var kravregisterCache = mutableListOf<KravWcag2x>()

  @Cacheable("kravFromSuksesskriterium", unless = "#result==null")
  fun getKrav(suksesskriterium: String): KravWcag2x {
    logger.info(
        "Henter krav fra ${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium .")
    return restClient
        .get()
        .uri("${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium")
        .retrieve()
        .body(KravWcag2x::class.java)
        ?: throw RuntimeException(
            "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
  }

  @Cacheable("kravFromId", unless = "#result==null")
  fun getWcagKrav(kravId: Int): KravWcag2x {
    return getKravregisterCache().associateBy { it.id }[kravId]
        ?: throw RuntimeException("Kravregisteret returnerte null for kravId $kravId")
  }

  @Cacheable("suksesskriteriumFromId", unless = "#result==null")
  fun getKravIdFromSuksesskritterium(suksesskriterium: String): Int {
    return getKravregisterCache().associateBy { it.suksesskriterium }[suksesskriterium]?.id
        ?: throw RuntimeException(
            "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
  }

  @Cacheable("suksesskriteriumFromKrav", unless = "#result == null")
  fun getSuksesskriteriumFromKrav(kravId: Int): String {
    return getWcagKrav(kravId).suksesskriterium
  }

  @Cacheable("kravList", unless = "#result==null")
  fun listKrav(): List<KravWcag2x> {
    return restClient
        .get()
        .uri("${properties.host}/v1/krav/wcag2krav")
        .retrieve()
        .body(object : ParameterizedTypeReference<List<KravWcag2x>>() {})
        ?: throw RuntimeException("Kravregisteret returnerte null for liste av krav")
  }

  fun getKravregisterCache(): List<KravWcag2x> {
    if (kravregisterCache.isEmpty()) {
      kravregisterCache = listKrav().toMutableList()
    }
    return kravregisterCache
  }
}

@ConfigurationProperties(prefix = "kravregister")
data class KravRegisterProperties(val host: String)
