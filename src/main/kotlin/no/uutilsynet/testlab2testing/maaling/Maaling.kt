package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import java.net.URI
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(Maaling.Planlegging::class, name = "planlegging"),
    JsonSubTypes.Type(Maaling.Crawling::class, name = "crawling"))
sealed class Maaling {
  abstract val navn: String
  abstract val id: Int
  abstract val aksjoner: List<Aksjon>

  data class Planlegging(
      override val id: Int,
      override val navn: String,
      val loeysingList: List<Loeysing>,
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf(Aksjon.StartCrawling(URI("${locationForId(id)}/status")))
  }

  data class Crawling(
      override val id: Int,
      override val navn: String,
      val crawlResultat: List<CrawlResultat>
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf()
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonTypeIdResolver(AksjonTypeIdResolver::class)
sealed class Aksjon(val metode: Metode, val data: Map<String, String>) {
  data class StartCrawling(val href: URI) : Aksjon(Metode.PUT, mapOf("status" to "crawling"))
}

enum class Metode {
  PUT
}

fun locationForId(id: Number): URI = URI.create("/v1/maalinger/${id}")

private class AksjonTypeIdResolver : TypeIdResolverBase() {
  override fun idFromValue(value: Any): String = idFromValueAndType(value, value::class.java)

  override fun idFromValueAndType(value: Any, suggestedType: Class<*>): String =
      when (value) {
        is Aksjon.StartCrawling -> "start_crawling"
        else -> throw RuntimeException("ukjent type: $suggestedType")
      }

  override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.NAME

  override fun typeFromId(context: DatabindContext, id: String): JavaType {
    val subtype =
        when (id) {
          "start_crawling" -> Aksjon.StartCrawling::class.java
          else -> throw RuntimeException("ukjent id: $id")
        }
    return context.constructType(subtype)
  }
}

fun validateNavn(s: String): Result<String> = runCatching {
  if (s == "") {
    throw IllegalArgumentException("mangler navn")
  } else {
    s
  }
}

enum class Status {
  Planlegging,
  Crawling
}

fun validateStatus(s: String?): Result<Status> =
    if (s == "crawling") {
      Result.success(Status.Crawling)
    } else {
      Result.failure(IllegalArgumentException("$s er ikke en gyldig status"))
    }
