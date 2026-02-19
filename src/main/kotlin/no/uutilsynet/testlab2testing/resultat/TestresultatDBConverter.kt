package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testresultat.TestresultatDB
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class TestresultatDBConverter(
    val testregelCache: TestregelCache,
    val sideutvalDAO: SideutvalDAO,
) {

  val logger: Logger = LoggerFactory.getLogger(TestresultatDBConverter::class.java)

  @Observed(name = "AutomatiskResultatService.mapTestresults")
  fun mapTestresults(list: List<TestresultatDB>): List<TestresultatDetaljert> {
    return list.parallelStream().map { it.toTestresultatDetaljert() }.collect(Collectors.toList())
  }

  private fun TestresultatDB.toTestresultatDetaljert(): TestresultatDetaljert {
    val testregel = testregelCache.getTestregelById(testregelId)
    val elementOmtale =
        TestresultatDetaljert.ElementOmtale(
            this.elmentOmtaleHtml, this.elementOmtalePointer, this.elementOmtaleDescription)
    return TestresultatDetaljert(
        this.id,
        this.loeysingId,
        testregelId,
        testregel.testregelId,
        this.maalingId ?: this.testgrunnlagId ?: 0,
        URI(this.side).toURL(),
        listOf(testregel.krav.suksesskriterium),
        LocalDateTime.ofInstant(this.testUtfoert, ZoneId.systemDefault()),
        this.elementUtfall,
        this.elementResultat,
        elementOmtale,
        null,
        null,
        null)
  }
}
