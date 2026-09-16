package no.uutilsynet.testlab2testing.forenkletkontroll

import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.model.TestregelBase
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class MaalingMapper(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val sideutvalDAO: SideutvalDAO,
    private val testkoeyringDAO: TestkoeyringDAO,
    private val testregelClient: TestregelClient,
) {

  private val logger = LoggerFactory.getLogger(MaalingMapper::class.java)

  fun toMaaling(maalingDbRow: MaalingDbRow): Maaling = MaalingContext(maalingDbRow).toMaaling()

  private inner class MaalingContext(private val maalingDbRow: MaalingDbRow) {
    private val loeysingar by lazy {
      getLoeysingarForMaaling(maalingDbRow.id, maalingDbRow.datoStart)
    }
    private val crawlResultat by lazy {
      sideutvalDAO.getCrawlResultatForMaaling(maalingDbRow.id, loeysingar)
    }
    private val testreglar by lazy { getTestregelListForMaaling(maalingDbRow.id) }
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

  private fun getLoeysingarForMaaling(id: Int, datoStart: Instant): List<Loeysing> {
    val loeysingIdList = getLoeysingIdsForMaaling(id)
    return loeysingsRegisterClient
        .getMany(loeysingIdList, datoStart)
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error(
                  "Feil ved henting av løysingar {} for maaling {}", loeysingIdList, id, it)
              throw it
            })
  }

  private fun getLoeysingIdsForMaaling(id: Int): List<Int> {
    val query =
        """select idloeysing from "testlab2_testing"."maalingloeysing" where idmaaling = :id"""
    return jdbcTemplate.queryForList(query, mapOf("id" to id), Int::class.java)
  }

  private fun getTestregelListForMaaling(maalingId: Int): List<TestregelBase> {
    val testregelIds = getTestregelIdsForMaaling(maalingId)
    return testregelClient.getTestregelListFromIds(testregelIds).getOrThrow().map {
      it.toTestregelBase()
    }
  }

  private fun getTestregelIdsForMaaling(maalingId: Int): List<Int> {
    return jdbcTemplate.queryForList(
        """select testregel_id from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""",
        mapOf("maalingId" to maalingId),
        Int::class.java)
  }

  private fun loeysingsMetadata(crawlResultat: List<CrawlResultat>): Map<Int, LoeysingMetadata> {
    return crawlResultat.filterIsInstance<CrawlResultat.Ferdig>().associate {
      it.loeysing.id to LoeysingMetadata(it.loeysing.id, it.loeysing, it.antallNettsider)
    }
  }
}
