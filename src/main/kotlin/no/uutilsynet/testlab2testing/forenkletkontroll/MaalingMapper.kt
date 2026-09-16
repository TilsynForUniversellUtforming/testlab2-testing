package no.uutilsynet.testlab2testing.forenkletkontroll

import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import org.springframework.stereotype.Component

@Component
class MaalingMapper(
    private val maalingReadService: MaalingReadService,
    private val sideutvalDAO: SideutvalDAO,
    private val testkoeyringDAO: TestkoeyringDAO,
) {

  fun toMaaling(maalingDbRow: MaalingDbRow): Maaling = MaalingContext(maalingDbRow).toMaaling()

  private inner class MaalingContext(private val maalingDbRow: MaalingDbRow) {
    private val loeysingar by lazy {
      maalingReadService.getLoeysingarForMaaling(maalingDbRow.id, maalingDbRow.datoStart)
    }
    private val crawlResultat by lazy {
      sideutvalDAO.getCrawlResultatForMaaling(maalingDbRow.id, loeysingar)
    }
    private val testreglar by lazy {
      maalingReadService.getTestregelBaseListForMaaling(maalingDbRow.id)
    }
    private val testkoeyringar by lazy {
      testkoeyringDAO.getTestKoeyringarForMaaling(maalingDbRow.id, loeysingsMetadata(crawlResultat))
    }

    fun toMaaling(): Maaling =
        with(maalingDbRow) {
          when (status) {
            MaalingStatus.planlegging ->
                Maaling.Planlegging(
                    id,
                    navn,
                    datoStart,
                    loeysingar,
                    testreglar,
                    CrawlParameters(maxLenker, talLenker))
            MaalingStatus.crawling -> Maaling.Crawling(id, navn, datoStart, crawlResultat)
            MaalingStatus.kvalitetssikring ->
                Maaling.Kvalitetssikring(id, navn, datoStart, crawlResultat)
            MaalingStatus.testing -> Maaling.Testing(id, navn, datoStart, testkoeyringar)
            MaalingStatus.testing_ferdig ->
                Maaling.TestingFerdig(id, navn, datoStart, testkoeyringar)
          }
        }
  }

  private fun loeysingsMetadata(crawlResultat: List<CrawlResultat>): Map<Int, LoeysingMetadata> {
    return crawlResultat.filterIsInstance<CrawlResultat.Ferdig>().associate {
      it.loeysing.id to LoeysingMetadata(it.loeysing.id, it.loeysing, it.antallNettsider)
    }
  }
}
