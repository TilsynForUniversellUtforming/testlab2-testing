package no.uutilsynet.testlab2testing.maaling

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.net.URI
import java.time.LocalDate
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.loeysing.Loeysing

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "status")
@JsonSubTypes(
    JsonSubTypes.Type(Maaling.Planlegging::class, name = "planlegging"),
    JsonSubTypes.Type(Maaling.Crawling::class, name = "crawling"),
    JsonSubTypes.Type(Maaling.Kvalitetssikring::class, name = "kvalitetssikring"),
    JsonSubTypes.Type(Maaling.Testing::class, name = "testing"),
    JsonSubTypes.Type(Maaling.TestingFerdig::class, name = "testing_ferdig"))
sealed class Maaling {
  abstract val id: Int
  abstract val navn: String
  abstract val datoStart: LocalDate
  abstract val aksjoner: List<Aksjon>

  data class Planlegging(
      override val id: Int,
      override val navn: String,
      override val datoStart: LocalDate,
      val loeysingList: List<Loeysing>,
      val testregelList: List<Testregel>,
      val crawlParameters: CrawlParameters
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf(Aksjon.StartCrawling(URI("${locationForId(id)}/status")))
  }

  data class Crawling(
      override val id: Int,
      override val navn: String,
      override val datoStart: LocalDate,
      val crawlResultat: List<CrawlResultat>
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf()
  }

  data class Kvalitetssikring(
      override val id: Int,
      override val navn: String,
      override val datoStart: LocalDate,
      val crawlResultat: List<CrawlResultat>,
      override val aksjoner: List<Aksjon> =
          listOf(
              Aksjon.RestartCrawling(URI("${locationForId(id)}/status")),
              Aksjon.StartTesting(URI("${locationForId(id)}/status")))
  ) : Maaling()

  data class Testing(
      override val id: Int,
      override val navn: String,
      override val datoStart: LocalDate,
      val testKoeyringar: List<TestKoeyring>,
      override val aksjoner: List<Aksjon> = listOf(),
  ) : Maaling()

  data class TestingFerdig(
      override val id: Int,
      override val navn: String,
      override val datoStart: LocalDate,
      val testKoeyringar: List<TestKoeyring>,
  ) : Maaling() {
    override val aksjoner: List<Aksjon>
      get() = listOf()
  }

  companion object {
    fun toTestingFerdig(maaling: Testing): TestingFerdig? {
      val allAreDone =
          maaling.testKoeyringar.all { it is TestKoeyring.Ferdig || it is TestKoeyring.Feila }
      return if (allAreDone) {
        TestingFerdig(maaling.id, maaling.navn, maaling.datoStart, maaling.testKoeyringar)
      } else {
        null
      }
    }

    fun toCrawling(planlagtMaaling: Planlegging, crawlResultat: List<CrawlResultat>): Crawling =
        Crawling(planlagtMaaling.id, planlagtMaaling.navn, planlagtMaaling.datoStart, crawlResultat)

    fun toKvalitetssikring(crawlingMaaling: Crawling): Kvalitetssikring? =
        if (crawlingMaaling.crawlResultat.any { it is CrawlResultat.IkkeFerdig }) {
          null
        } else {
          Kvalitetssikring(
              crawlingMaaling.id,
              crawlingMaaling.navn,
              crawlingMaaling.datoStart,
              crawlingMaaling.crawlResultat)
        }

    fun toTesting(maaling: Kvalitetssikring, testKoeyringar: List<TestKoeyring>): Testing {
      return Testing(maaling.id, maaling.navn, maaling.datoStart, testKoeyringar)
    }

    fun findFerdigeTestKoeyringar(
        maaling: Maaling,
        loeysingId: Int? = null
    ): List<TestKoeyring.Ferdig> {
      val testKoeyringar =
          when (maaling) {
            is TestingFerdig -> maaling.testKoeyringar
            is Testing -> maaling.testKoeyringar
            else -> emptyList()
          }

      return testKoeyringar.filterIsInstance<TestKoeyring.Ferdig>().filter {
        loeysingId == null || it.crawlResultat.loeysing.id == loeysingId
      }
    }
  }
}

// Enkel representasjon av måling som brukes i utlisting av alle målinger.
data class MaalingListElement(
    val id: Int,
    val navn: String,
    val datoStart: LocalDate,
    val status: String
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "id")
@JsonSubTypes(
    JsonSubTypes.Type(value = Aksjon.StartCrawling::class, name = "start_crawling"),
    JsonSubTypes.Type(value = Aksjon.RestartCrawling::class, name = "restart_crawling"),
    JsonSubTypes.Type(value = Aksjon.StartTesting::class, name = "start_testing"),
    JsonSubTypes.Type(value = Aksjon.RestartTesting::class, name = "restart_testing"))
sealed class Aksjon(val data: Map<String, String>) {
  val metode = "PUT"

  data class StartCrawling(val href: URI) : Aksjon(mapOf("status" to "crawling"))
  data class RestartCrawling(val href: URI) :
      Aksjon(mapOf("status" to "crawling", "loeysingIdList" to "[]"))
  data class StartTesting(val href: URI) : Aksjon(mapOf("status" to "testing"))
  data class RestartTesting(val href: URI) :
      Aksjon(mapOf("status" to "testing", "loeysingIdList" to "[]"))
}

fun locationForId(id: Number): URI = URI.create("/v1/maalinger/${id}")

enum class Status {
  Crawling,
  Testing
}
