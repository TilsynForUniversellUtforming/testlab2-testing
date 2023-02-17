package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import java.net.URI
import java.net.URL

sealed class Maaling {
  data class Planlegging(val id: Int, val navn: String, val url: URL, val aksjoner: List<Aksjon>) :
      Maaling()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonTypeIdResolver(AksjonTypeIdResolver::class)
sealed class Aksjon(val metode: Metode, val data: Map<String, String>) {
  data class StartCrawling(val href: URI) : Aksjon(Metode.POST, mapOf("status" to "crawling"))
}

enum class Metode {
  POST
}

fun locationForId(id: Number): URI = URI.create("/v1/maalinger/${id}")

private class AksjonTypeIdResolver : TypeIdResolverBase() {
  override fun idFromValue(value: Any): String = idFromValueAndType(value, value::class.java)

  override fun idFromValueAndType(value: Any, suggestedType: Class<*>): String =
      when (value) {
        is Aksjon.StartCrawling -> "start_crawling"
        else -> throw RuntimeException("ukjent type: ${suggestedType}")
      }

  override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.NAME

  override fun typeFromId(context: DatabindContext, id: String): JavaType {
    val subtype =
        when (id) {
          "start_crawling" -> Aksjon.StartCrawling::class.java
          else -> throw RuntimeException("ukjent id: ${id}")
        }
    return context.constructType(subtype)
  }
}
