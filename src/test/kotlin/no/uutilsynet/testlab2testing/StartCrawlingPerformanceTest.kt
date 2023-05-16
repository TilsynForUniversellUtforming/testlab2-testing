package no.uutilsynet.testlab2testing

import java.net.URL
import java.time.Duration
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO
import no.uutilsynet.testlab2testing.maaling.CrawlParameters
import no.uutilsynet.testlab2testing.maaling.MaalingDAO
import no.uutilsynet.testlab2testing.maaling.MaalingResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource

fun <T> randomSample(sampleSize: Int, collection: List<T>): List<T> =
    collection.shuffled().take(sampleSize)

fun <T> timeExecution(function: () -> T): Pair<T, Duration> {
  val startTime = System.currentTimeMillis()
  val result = function()
  val endTime = System.currentTimeMillis()
  return Pair(result, Duration.ofMillis(endTime - startTime))
}

// En test som sjekker at det ikke tar for lang tid å starte crawling av en måling med mange
// løsninger.
// Denne testen forventer at det kjører en test fake på http://127.0.0.1:3000/crawler. Den kan
// startes ved å kjøre fake-crawler/server.js.
@SpringBootTest
@TestPropertySource(properties = ["crawler.url=http://127.0.0.1:3000/crawler"])
@Disabled
class StartCrawlingPerformanceTest(
    @Autowired val maalingDAO: MaalingDAO,
    @Autowired val maalingResource: MaalingResource,
    @Autowired val loeysingDAO: LoeysingDAO
) {

  @DisplayName("gitt en måling med status planlegging og 200 løsninger")
  @Nested
  inner class EnStorMaaling {
    private val loeysingList: List<Loeysing> = loeysingDAO.getLoeysingList()
    private val loeysingSample: List<Loeysing> = randomSample(200, loeysingList)
    private val maalingId: Int =
        maalingDAO.createMaaling(
            "performance_test_maaling", loeysingSample.map { it.id }, CrawlParameters())

    @DisplayName("når vi starter crawling, skal den returnere innen 10 sekunder")
    @Test
    fun starterCrawling() {
      try {
        val (result, duration) =
            timeExecution {
              maalingResource.putNewStatus(maalingId, MaalingResource.StatusDTO("crawling", null))
            }
        println("Sendte 200 requests på ${duration.toMillis()} ms")
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(duration).isLessThan(Duration.ofSeconds(10))
      } finally {
        maalingDAO.deleteMaaling(maalingId)
      }
    }
  }

  companion object {
    private var loeysingIdList: List<Int> = emptyList()

    @JvmStatic
    @BeforeAll
    fun setup(@Autowired loeysingDAO: LoeysingDAO) {
      val ids = mutableListOf<Int>()
      (1..1000).forEach { i ->
        val id =
            loeysingDAO.createLoeysing("performance_test_loeysing_$i", URL("http://loeysing$i.com"))
        ids.add(id)
      }
      loeysingIdList = ids.toList()
    }

    @JvmStatic
    @AfterAll
    fun teardown(@Autowired loeysingDAO: LoeysingDAO) {
      loeysingIdList.forEach { id -> loeysingDAO.deleteLoeysing(id) }
    }
  }
}
