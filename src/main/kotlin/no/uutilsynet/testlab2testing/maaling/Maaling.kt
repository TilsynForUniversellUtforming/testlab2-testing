package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(Maaling.Planlegging::class, name = "planlegging"),
    JsonSubTypes.Type(Maaling.Crawling::class, name = "crawling"),
    JsonSubTypes.Type(Maaling.Kvalitetssikring::class, name = "kvalitetssikring"))
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

  data class Kvalitetssikring(
      override val id: Int,
      override val navn: String,
      val crawlResultat: List<CrawlResultat>
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf()
  }

  companion object {
    fun toCrawling(planlagtMaaling: Planlegging, crawlResultat: List<CrawlResultat>): Crawling =
        Crawling(planlagtMaaling.id, planlagtMaaling.navn, crawlResultat)

    fun toKvalitetssikring(crawlingMaaling: Crawling): Kvalitetssikring? =
        if (crawlingMaaling.crawlResultat.any { it is CrawlResultat.IkkeFerdig }) {
          null
        } else {
          Kvalitetssikring(crawlingMaaling.id, crawlingMaaling.navn, crawlingMaaling.crawlResultat)
        }
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonSubTypes(JsonSubTypes.Type(value = Aksjon.StartCrawling::class, name = "start_crawling"))
sealed class Aksjon(val metode: Metode, val data: Map<String, String>) {
  data class StartCrawling(val href: URI) : Aksjon(Metode.PUT, mapOf("status" to "crawling"))
}

enum class Metode {
  PUT
}

fun locationForId(id: Number): URI = URI.create("/v1/maalinger/${id}")

fun validateNavn(s: String): Result<String> = runCatching {
  if (s == "") {
    throw IllegalArgumentException("mangler navn")
  } else {
    s
  }
}

enum class Status {
  Crawling
}

fun validateStatus(s: String?): Result<Status> =
    if (s == "crawling") {
      Result.success(Status.Crawling)
    } else {
      Result.failure(IllegalArgumentException("$s er ikke en gyldig status"))
    }
