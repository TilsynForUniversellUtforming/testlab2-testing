package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URL
import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest // (classes = [AggregeringDAO::class])
class AggregeringDAOTest(@Autowired val aggregeringDAO: AggregeringDAO) {

  @MockBean lateinit var loeysingsRegisterClient: LoeysingsRegisterClient

  @MockBean lateinit var autoTesterClient: AutoTesterClient

  var aggregeringTestregel: AggregertResultatTestregel =
      AggregertResultatTestregel(
          maalingId = 1,
          loeysing = Loeysing(1, "test", URL("http://localhost:8080/"), "123456789"),
          testregelId = "QW-1",
          suksesskriterium = "1.1.1",
          fleireSuksesskriterium =
              listOf("1.1.1","1.1.2"),
          talElementSamsvar = 1,
          talElementBrot = 2,
          talElementVarsel = 1,
          talElementIkkjeForekomst = 1,
          talSiderSamsvar = 1,
          talSiderBrot = 1,
          talSiderIkkjeForekomst = 1,
          testregelGjennomsnittlegSideSamsvarProsent = 1.0f,
          testregelGjennomsnittlegSideBrotProsent = 1.0f)

  @Test
  fun saveAggregeringTestregel() {
    val testKoeyring: TestKoeyring.Ferdig =
        TestKoeyring.Ferdig(
            crawlResultat =
                CrawlResultat.Ferdig(
                    antallNettsider = 1,
                    statusUrl = URL("http://localhost:8080/"),
                    loeysing = Loeysing(1, "test", URL("http://localhost:8080/"), "123456789"),
                    sistOppdatert = Instant.now(),
                    nettsider = emptyList()),
            sistOppdatert = Instant.now(),
            statusURL = URL("http://localhost:8080/"),
            testResultat = emptyList(),
            lenker =
                AutoTesterClient.AutoTesterOutput.Lenker(
                    urlFulltResultat = URL("http://localhost:8080/"),
                    urlBrot = URL("http://localhost:8080/"),
                    urlAggregeringSideTR = URL("http://localhost:8080/"),
                    urlAggregeringTR = URL("http://localhost:8080/"),
                    urlAggregeringSide = URL("http://localhost:8080/"),
                    urlAggregeringLoeysing = URL("http://localhost:8080/"),
                    urlAggregeringSK = URL("http://localhost:8080/")),
        )


      Mockito.`when`(autoTesterClient. fetchResultatAggregering(URL("http://localhost:8080/").toURI(),AutoTesterClient.ResultatUrls.urlAggreggeringTR)).thenReturn(
          listOf(aggregeringTestregel)
      );
    // AutoTesterClient.ResultatUrls.urlAggreggeringTR)).thenReturn(aggregeringTestregel)
    aggregeringDAO.saveAggregertResultatTestregel(testKoeyring)
  }
}
