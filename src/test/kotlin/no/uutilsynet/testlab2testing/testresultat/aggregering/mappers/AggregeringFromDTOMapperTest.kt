package no.uutilsynet.testlab2testing.testresultat.aggregering.mappers

import java.net.URI
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AggregeringFromDTOMapperTest {

  private val testregelCache = Mockito.mock(TestregelCache::class.java)
  private val kravregisterClient = Mockito.mock(KravregisterClient::class.java)
  private val mapper = AggregeringFromDTOMapper(testregelCache, kravregisterClient)

  @Test
  fun dtoToAggregertResultatTestregelFallsBackToNumericIdWhenCacheMisses() {
    Mockito.`when`(testregelCache.getTestregelById(123))
        .thenThrow(NoSuchElementException("Testregel not found for id: 123"))
    Mockito.`when`(kravregisterClient.getSuksesskriteriumFromKrav(1)).thenReturn("1.1.1")

    val result =
        mapper.dtoToAggregertResultatTestregel(
            AggregeringPerTestregelDB(
                maalingId = null,
                loeysingId = 1,
                testregelId = 123,
                suksesskriterium = 1,
                fleireSuksesskriterium = listOf(1),
                talElementSamsvar = 1,
                talElementBrot = 0,
                talElementVarsel = 0,
                talElementIkkjeForekomst = 0,
                talSiderSamsvar = 1,
                talSiderBrot = 0,
                talSiderIkkjeForekomst = 0,
                testregelGjennomsnittlegSideSamsvarProsent = 1.0,
                testregelGjennomsnittlegSideBrotProsent = 0.0,
                testgrunnlagId = 7),
            listOf(Loeysing(1, "test", URI("http://example.com").toURL(), "123456789", "Test AS")))

    assertThat(result.testregelId).isEqualTo("123")
    assertThat(result.suksesskriterium).isEqualTo("1.1.1")
  }
}
