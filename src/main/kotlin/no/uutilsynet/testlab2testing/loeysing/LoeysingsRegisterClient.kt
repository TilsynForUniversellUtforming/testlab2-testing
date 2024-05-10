package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@ConfigurationProperties(prefix = "loeysingsregister")
data class LoeysingsRegisterProperties(val host: String)

@Component
class LoeysingsRegisterClient(
    val restTemplate: RestTemplate,
    val properties: LoeysingsRegisterProperties
) {
  val logger: Logger = LoggerFactory.getLogger(LoeysingsRegisterClient::class.java)

  @CacheEvict(key = "#result.id", cacheNames = ["loeysing", "loeysingar"])
  fun saveLoeysing(namn: String, url: URL, orgnummer: String): Result<Loeysing> = runCatching {
    val location =
        restTemplate.postForLocation(
            "${properties.host}/v1/loeysing",
            mapOf("namn" to namn, "url" to url.toString(), "orgnummer" to orgnummer))
    location?.let { restTemplate.getForObject(it, Loeysing::class.java) }
        ?: throw RuntimeException(
            "loeysingsregisteret returnerte ikkje ein location da vi oppretta ei ny løysing")
  }

  @Cacheable("loeysingar", unless = "#result == Result.Failure")
  fun getMany(idList: List<Int>): Result<List<Loeysing>> = getMany(idList, Instant.now())

  fun getMany(idList: List<Int>, tidspunkt: Instant): Result<List<Loeysing>> {
    return runCatching {
      if (idList.isEmpty()) {
        emptyList()
      } else {
        val uri =
            UriComponentsBuilder.fromUriString(properties.host)
                .pathSegment("v1", "loeysing")
                .queryParam("ids", idList.joinToString(","))
                .queryParam("atTime", ISO_INSTANT.format(tidspunkt))
                .build()
                .toUri()
        restTemplate.getForObject(uri, Array<Loeysing>::class.java)?.toList()
            ?: throw RuntimeException(
                "loeysingsregisteret returnerte null for id-ane ${idList.joinToString(",")}")
      }
    }
  }

  fun search(search: String): Result<List<Loeysing>> {
    return runCatching {
      val uri =
          UriComponentsBuilder.fromUriString(properties.host)
              .pathSegment("v1", "loeysing")
              .queryParam("search", search)
              .build()
              .toUri()
      restTemplate.getForObject(uri, Array<Loeysing>::class.java)?.toList()
          ?: throw RuntimeException("loeysingsregisteret returnerte null for søk $search")
    }
  }

  @CacheEvict(key = "#id", cacheNames = ["loeysing", "loeysingar"])
  fun delete(id: Int): Result<Unit> = runCatching {
    restTemplate.delete("${properties.host}/v1/loeysing/$id")
  }

  @Cacheable("loeysing", unless = "#result?.id==null")
  fun getLoeysingFromId(loeysingId: Int): Result<Loeysing> {
    return runCatching {
      getMany(listOf(loeysingId)).let { loeysingList ->
        loeysingList.getOrThrow().firstOrNull()?.let {
          return Result.success(it)
        }
      }
      throw RuntimeException("Fant ikkje løsning med id $loeysingId")
    }
  }
}
