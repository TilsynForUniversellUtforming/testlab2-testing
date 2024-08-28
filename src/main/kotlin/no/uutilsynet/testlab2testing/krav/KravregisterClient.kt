package no.uutilsynet.testlab2testing.krav

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class KravregisterClient(val restTemplate: RestTemplate, val properties: KravRegisterProperties) {

  @Cacheable("kravFromSuksesskriterium", unless = "#result==null")
  fun getKrav(suksesskriterium: String): KravWcag2x {
    logger.info(
        "Henter krav fra ${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium .")
    return runCatching {
          restTemplate.getForObject(
              "${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium",
              KravWcag2x::class.java)
              ?: throw RuntimeException(
                  "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
        }
        .getOrThrow()
  }

  @Cacheable("kravFromId", unless = "#result==null")
  fun getWcagKrav(kravId: Int): KravWcag2x {
    return runCatching {
          restTemplate.getForObject(
              "${properties.host}/v1/krav/wcag2krav/$kravId", KravWcag2x::class.java)
              ?: throw RuntimeException("Kravregisteret returnerte null for kravId $kravId")
        }
        .getOrThrow()
  }

  @Cacheable("suksesskriteriumFromId", unless = "#result==null")
  fun getKravIdFromSuksesskritterium(suksesskriterium: String): Int {
    return getKrav(suksesskriterium).id
  }

  @Cacheable("suksesskriteriumFromKrav", unless = "#result == null")
  fun getSuksesskriteriumFromKrav(kravId: Int): String {
    return runCatching { getWcagKrav(kravId).suksesskriterium }.getOrThrow()
  }
}

@ConfigurationProperties(prefix = "kravregister")
data class KravRegisterProperties(val host: String)
