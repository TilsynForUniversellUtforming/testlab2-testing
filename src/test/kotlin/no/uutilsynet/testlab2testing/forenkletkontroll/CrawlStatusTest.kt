package no.uutilsynet.testlab2testing.forenkletkontroll

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CrawlStatusTest {
  @DisplayName("når responsen fra crawleren er `Terminated`, så skal det parses til responsklassen")
  @Test
  fun terminated() {
    val jsonString = """{"runtimeStatus":"Terminated"}"""
    val terminated = jacksonObjectMapper().readValue(jsonString, CrawlStatus::class.java)
    assertThat(terminated).isInstanceOf(CrawlStatus.Terminated::class.java)
  }
}
