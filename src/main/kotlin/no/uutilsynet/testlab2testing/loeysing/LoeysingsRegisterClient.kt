package no.uutilsynet.testlab2testing.loeysing

import io.micrometer.observation.annotation.Observed
import jakarta.validation.ClockProvider
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_INSTANT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder

@ConfigurationProperties(prefix = "loeysingsregister")
data class LoeysingsRegisterProperties(val host: String)

private const val LOEYSINGSREGISTER_NEW_NOT_FOUND =
    "loeysingsregisteret returnerte ikkje ein location da vi oppretta ei ny løysing"

@Component
class LoeysingsRegisterClient(
    val restTemplate: RestTemplate,
    val properties: LoeysingsRegisterProperties,
    val clockProvider: ClockProvider
) {
  val logger: Logger = LoggerFactory.getLogger(LoeysingsRegisterClient::class.java)

  @CacheEvict(
      key = "#result.id", cacheNames = ["loeysing", "loeysingar"], condition = "#result!=null")
  fun saveLoeysing(namn: String, url: URL, orgnummer: String): Loeysing =
      runCatching {
            val location =
                RestClient.create()
                    .post()
                    .uri("${properties.host}/v1/loeysing")
                    .body(mapOf("namn" to namn, "url" to url.toString(), "orgnummer" to orgnummer))
                    .retrieve()
                    .toBodilessEntity()
                    .headers
                    .location
                    ?: throw NoSuchElementException(LOEYSINGSREGISTER_NEW_NOT_FOUND)

            RestClient.create().get().uri(location).retrieve().body<Loeysing.Simple>()?.toLoeysing()
                ?: throw NoSuchElementException(LOEYSINGSREGISTER_NEW_NOT_FOUND)
          }
          .getOrThrow()

  @Cacheable("loeysingar", unless = "#result==null")
  fun getMany(idList: List<Int>): Result<List<Loeysing>> =
      getMany(idList, Instant.now(clockProvider.clock))

  @Cacheable("loeysingar", unless = "#result==null")
  fun getMany(idList: List<Int>, tidspunkt: Instant): Result<List<Loeysing>> {
    logger.info("Getting loeysing for ids: {} at {}", idList.size, tidspunkt)
    return getManyExpanded(idList, tidspunkt)
        .getOrThrow()
        .map { loeysing -> loeysing.toLoeysing() }
        .let { Result.success(it) }
  }

  @Cacheable("loeysingar", unless = "#result==null")
  fun getManyWithoutVerksemd(idList: List<Int>, tidspunkt: Instant): Result<List<Loeysing>> {
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

        RestClient.create().get().uri(uri).retrieve().body<Array<Loeysing>>()?.toList()
            ?: throw NoSuchElementException(
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

      RestClient.create().get().uri(uri).retrieve().body<Array<Loeysing.Simple>>()?.map {
        it.toLoeysing()
      }
          ?: throw NoSuchElementException("loeysingsregisteret returnerte null for søk $search")
    }
  }

  @CacheEvict(key = "#id", cacheNames = ["loeysing", "loeysingar"])
  fun delete(id: Int): Result<Unit> = runCatching {
    RestClient.create()
        .delete()
        .uri("${properties.host}/v1/loeysing/$id")
        .retrieve()
        .toBodilessEntity()
  }

  @Cacheable("loeysing", unless = "#result==null")
  fun getLoeysingFromId(loeysingId: Int): Loeysing {
    return getMany(listOf(loeysingId)).mapCatching { it.first() }.getOrThrow()
  }

  @Cacheable("loeysingarExpanded")
  @Observed(
      name = "LoeysingsRegisterClient.getManyExpanded",
      contextualName = "LoeysingsRegisterClient.getManyExpanded")
  fun getManyExpanded(
      idList: List<Int>,
      tidspunkt: Instant = Instant.now(clockProvider.clock),
  ): Result<List<Loeysing.Expanded>> {
    logger.debug("Getting expanded loeysing for ids: {} at {}", idList.size, tidspunkt)
    val unique = idList.toSet().toList()

    return runCatching {
      if (idList.isEmpty()) {
        emptyList()
      } else {
        val uri =
            UriComponentsBuilder.fromUriString(properties.host)
                .pathSegment("v1", "loeysing", "expanded")
                .queryParam("ids", unique.joinToString(","))
                .queryParam("atTime", Instant.now(clockProvider.clock))
                .build()
                .toUri()

        RestClient.create().get().uri(uri).retrieve().body<Array<Loeysing.Expanded>>()?.toList()
            ?: throw NoSuchElementException(
                "loeysingsregisteret returnerte null for id-ane ${unique.joinToString(",")}")
      }
    }
  }

  fun getManyExpanded(id: List<Int>): Result<List<Loeysing.Expanded>> {
    return getManyExpanded(id, Instant.now(clockProvider.clock))
  }

  fun searchVerksemd(search: String): Result<List<Verksemd>> {
    return runCatching {
      val uri =
          UriComponentsBuilder.fromUriString(properties.host)
              .pathSegment("v1", "verksemd", "list")
              .queryParam("search", search)
              .queryParam("atTime", ISO_INSTANT.format(Instant.now(clockProvider.clock)))
              .build()
              .toUriString()

      logger.info("SearchVerkemd uri: $uri")

      RestClient.create().get().uri(uri).retrieve().body<Array<Verksemd>>()?.toList()
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
              .queryParam("atTime", ISO_INSTANT.format(Instant.now(clockProvider.clock)))
              .build()
              .toUriString()

      RestClient.create().get().uri(uri).retrieve().body<Array<Loeysing>>()?.toList()
          ?: throw NoSuchElementException(
              "loeysingsregisteret returnerte null for verksemdsøk $search")
    }
  }

  fun Loeysing.Expanded.toLoeysing(): Loeysing {
    if (verksemd == null) {
      logger.warn("Loeysing $id manglar verksemd")
      val simple =
          getManyWithoutVerksemd(listOf(id), Instant.now(clockProvider.clock))
              .getOrThrow()
              .firstOrNull()
      simple?.let {
        return Loeysing(id, namn, url, simple.orgnummer, "Manglar verksemd")
      }
    } else {
      return Loeysing(id, namn, url, verksemd.organisasjonsnummer, verksemd.namn)
    }
    throw NoSuchElementException("Klarte ikkje hente loeysing med id $id")
  }

  fun Loeysing.Simple.toLoeysing(): Loeysing {
    return Loeysing(id, namn, url, orgnummer, null)
  }
}
