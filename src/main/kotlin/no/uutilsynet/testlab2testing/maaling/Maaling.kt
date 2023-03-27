package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import no.uutilsynet.testlab2testing.dto.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(Maaling.Planlegging::class, name = "planlegging"),
    JsonSubTypes.Type(Maaling.Crawling::class, name = "crawling"),
    JsonSubTypes.Type(Maaling.Kvalitetssikring::class, name = "kvalitetssikring"),
    JsonSubTypes.Type(Maaling.Testing::class, name = "testing"))
sealed class Maaling {
  abstract val id: Int
  abstract val navn: String
  abstract val aksjoner: List<Aksjon>

  data class Planlegging(
      override val id: Int,
      override val navn: String,
      val loeysingList: List<Loeysing>,
      val crawlParameters: CrawlParameters
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
      val crawlResultat: List<CrawlResultat>,
      override val aksjoner: List<Aksjon> =
          listOf(
              Aksjon.RestartCrawling(URI("${locationForId(id)}/status")),
              Aksjon.StartTesting(URI("${locationForId(id)}/status")))
  ) : Maaling()

  data class Testing(
      override val id: Int,
      override val navn: String,
      val testKoeyringar: List<TestKoeyring>,
      override val aksjoner: List<Aksjon> = listOf(),
  ) : Maaling()

  companion object {
    fun toCrawling(planlagtMaaling: Planlegging, crawlResultat: List<CrawlResultat>): Crawling =
        Crawling(planlagtMaaling.id, planlagtMaaling.navn, crawlResultat)

    fun toKvalitetssikring(crawlingMaaling: Crawling): Kvalitetssikring? =
        if (crawlingMaaling.crawlResultat.any { it is CrawlResultat.IkkeFerdig }) {
          null
        } else {
          Kvalitetssikring(crawlingMaaling.id, crawlingMaaling.navn, crawlingMaaling.crawlResultat)
        }

    fun toTesting(maaling: Kvalitetssikring, testKoeyringar: List<TestKoeyring>): Testing {
      return Testing(maaling.id, maaling.navn, testKoeyringar)
    }
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonSubTypes(
    JsonSubTypes.Type(value = Aksjon.StartCrawling::class, name = "start_crawling"),
    JsonSubTypes.Type(value = Aksjon.RestartCrawling::class, name = "restart_crawling"),
    JsonSubTypes.Type(value = Aksjon.StartTesting::class, name = "start_testing"))
sealed class Aksjon(val data: Map<String, String>) {
  val metode = "PUT"

  data class StartCrawling(val href: URI) : Aksjon(mapOf("status" to "crawling"))
  data class RestartCrawling(val href: URI) :
      Aksjon(mapOf("status" to "crawling", "loeysingIdList" to "[]"))
  data class StartTesting(val href: URI) : Aksjon(mapOf("status" to "testing"))
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
  Crawling,
  Testing
}

fun validateStatus(s: String?): Result<Status> =
    when (s) {
      "crawling" -> Result.success(Status.Crawling)
      "testing" -> Result.success(Status.Testing)
      else -> Result.failure(IllegalArgumentException("$s er ikke en gyldig status"))
    }

fun validateLoeysingIdList(list: List<Int>?): Result<List<Int>> = runCatching {
  if (list.isNullOrEmpty()) {
    throw IllegalArgumentException(
        "Eg forventa eit parameter `loeysingIdList` som skulle inneholde ei liste med id-ar, men han var tom.")
  }
  list
}
