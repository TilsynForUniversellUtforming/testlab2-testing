package no.uutilsynet.testlab2testing.loeysing

import io.micrometer.observation.annotation.Observed
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT

@ConfigurationProperties(prefix = "loeysingsregister")
data class LoeysingsRegisterProperties(val host: String)

@Component
class LoeysingsRegisterClient (
    val restTemplate: RestTemplate,
    val properties: LoeysingsRegisterProperties
): LoeysingRegisterAPI {
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

  @Cacheable("loeysingar", unless = "!#result.isSuccess")
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
              .toUriString()
      restTemplate.getForObject(uri, Array<Loeysing>::class.java)?.toList()
          ?: throw RuntimeException("loeysingsregisteret returnerte null for søk $search")
    }
  }

  @CacheEvict(key = "#id", cacheNames = ["loeysing", "loeysingar"])
  fun delete(id: Int): Result<Unit> = runCatching {
    restTemplate.delete("${properties.host}/v1/loeysing/$id")
  }

  @Cacheable("loeysing", unless = "#result==null")
  fun getLoeysingFromId(loeysingId: Int): Loeysing {
    return getMany(listOf(loeysingId)).mapCatching { it.first() }.getOrThrow()
  }

  @Cacheable("loeysingarExpanded")
  @Observed(
      name = "LoeysingsRegisterClient.getManyExpanded",
      contextualName = "LoeysingsRegisterClient.getManyExpanded"
  )
  fun getManyExpanded(idList: List<Int>): Result<List<Loeysing.Expanded>> {
      logger.info("Getting expanded loeysing for ids: ${idList.size}")
      val unique = idList.toSet().toList()

      return runCatching {
      val uri =
          UriComponentsBuilder.fromUriString(properties.host)
              .pathSegment("v1", "loeysing", "expanded")
              .queryParam("ids", unique.joinToString(","))
              .build()
              .toUri()
        val response = restTemplate.getForObject(uri, Array<Loeysing.Expanded>::class.java)?.toList()
          ?.toList()
          ?: throw RuntimeException(
              "loeysingsregisteret returnerte null for id-ane ${unique.joinToString(",")}")

        logger.info("Loeysing size " +  response.size)
        response
    }
  }

  fun searchVerksemd(search: String): Result<List<Verksemd>> {
    return runCatching {
      val uri =
          UriComponentsBuilder.fromUriString(properties.host)
              .pathSegment("v1", "verksemd", "list")
              .queryParam("search", search)
              .queryParam("atTime", ISO_INSTANT.format(Instant.now()))
              .build()
              .toUriString()

      logger.info("SearchVerkemd uri: $uri")
      restTemplate.getForObject(uri, Array<Verksemd>::class.java)?.toList()
          ?: throw NoSuchElementException(
              "loeysingsregisteret returnerte null for verksemdsøk $search")
    }
  }

  fun searchLoeysingByVerksemd(search: String): Result<List<Loeysing>> {
    return kotlin.runCatching {
      val uri =
          UriComponentsBuilder.fromUriString(properties.host)
              .pathSegment("v1", "loeysing", "verksemd")
              .queryParam("search", search)
              .queryParam("atTime", ISO_INSTANT.format(Instant.now()))
              .build()
              .toUriString()
      restTemplate.getForObject(uri, Array<Loeysing>::class.java)?.toList()
          ?: throw RuntimeException("loeysingsregisteret returnerte null for verksemdsøk $search")
    }
  }
}
