package no.uutilsynet.testlab2testing.krav

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class KravregisterClient(val restTemplate: RestTemplate, val properties: KravRegisterProperties) {

  @Cacheable("kravFromSuksesskriterium", unless = "#result?.id==null")
  fun getKrav(suksesskriterium: String): Result<KravWcag2x> {
    logger.info(
        "Henter krav fra ${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium .")
    return runCatching {
      restTemplate.getForObject(
          "${properties.host}/v1/krav/wcag2krav/suksesskriterium/$suksesskriterium",
          KravWcag2x::class.java)
          ?: throw RuntimeException(
              "Kravregisteret returnerte null for suksesskriterium $suksesskriterium")
    }
  }

  @Cacheable("kravFromId", unless = "#result?.id==null")
  fun getWcagKrav(kravId: Int): Result<KravWcag2x> {
    return runCatching {
      restTemplate.getForObject(
          "${properties.host}/v1/krav/wcag2krav/$kravId", KravWcag2x::class.java)
          ?: throw RuntimeException("Kravregisteret returnerte null for kravId $kravId")
    }
  }

  // @Cacheable("suksesskriteriumFromId", unless = "#result==null")
  fun getKravIdFromSuksesskritterium(suksesskriterium: String): Result<Int> {
    return runCatching {
      val krav = getKrav(suksesskriterium).getOrThrow()
      return Result.success(krav.id)
    }
  }

  @Cacheable("suksesskriteriumFromKrav", unless = "#result == null")
  fun getSuksesskriteriumFromKrav(kravId: Int): Result<String> {
    return runCatching { getWcagKrav(kravId).getOrThrow().suksesskriterium }
  }
}

@ConfigurationProperties(prefix = "kravregister")
data class KravRegisterProperties(val host: String)
