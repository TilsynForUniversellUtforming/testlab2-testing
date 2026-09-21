package no.uutilsynet.testlab2testing.forenkletkontroll

import java.time.Instant
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlParameters
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.model.TestregelBase
import org.slf4j.LoggerFactory
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service

@Service
class MaalingReadService(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val loeysingsRegisterClient: LoeysingsRegisterClient,
    private val testregelClient: TestregelClient,
) {

  private val logger = LoggerFactory.getLogger(MaalingReadService::class.java)
  private val crawlParametersRowmapper = DataClassRowMapper.newInstance(CrawlParameters::class.java)

  fun getLoeysingIdsForMaaling(maalingId: Int): List<Int> {
    val query =
        """select idloeysing from "testlab2_testing"."maalingloeysing" where idmaaling = :id"""
    return jdbcTemplate.queryForList(query, mapOf("id" to maalingId), Int::class.java)
  }

  fun getLoeysingarForMaaling(maalingId: Int, datoStart: Instant): List<Loeysing> {
    val loeysingIdList = getLoeysingIdsForMaaling(maalingId)
    return loeysingsRegisterClient
        .getMany(loeysingIdList, datoStart)
        .fold(
            onSuccess = { it },
            onFailure = {
              logger.error(
                  "Feil ved henting av løysingar {} for maaling {}", loeysingIdList, maalingId, it)
              throw it
            })
  }

  fun getLoeysingarForMaaling(maalingId: Int): List<Loeysing> =
      getLoeysingarForMaaling(maalingId, Instant.now())

  fun getTestregelIdsForMaaling(maalingId: Int): List<Int> {
    return jdbcTemplate.queryForList(
        """select testregel_id from "testlab2_testing"."maaling_testregel" where maaling_id = :maalingId""",
        mapOf("maalingId" to maalingId),
        Int::class.java)
  }

  fun getTestregelBaseListForMaaling(maalingId: Int): List<TestregelBase> {
    val testregelIds = getTestregelIdsForMaaling(maalingId)
    return testregelClient.getTestregelListFromIds(testregelIds).getOrThrow().map {
      it.toTestregelBase()
    }
  }

  fun getCrawlParameters(maalingId: Int): CrawlParameters {
    val query =
        """select m.max_lenker, m.tal_lenker from testlab2_testing."maalingv1" m where m.id = :id"""
    return runCatching {
          jdbcTemplate.queryForObject(query, mapOf("id" to maalingId), crawlParametersRowmapper)
              ?: throw NoSuchElementException("Fant ikke crawlparametere for maaling $maalingId")
        }
        .getOrElse {
          logger.error(
              "Kunne ikke hente crawlparametere for maaling {}, velger default parametere",
              maalingId,
              it)
          throw it
        }
  }

  fun getMaalingIdFromKontrollId(kontrollId: Int): Int? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              """select id from "testlab2_testing"."maalingv1" where kontrollId = :kontrollId""",
              mapOf("kontrollId" to kontrollId),
          ) { rs, _ ->
            rs.getInt("id")
          })

  fun getTestrunUuidForMaaling(maalingId: Int): Result<String> {
    return runCatching {
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              """select uuid from "testlab2_testing"."maalingv1" where id = :maalingId""",
              mapOf("maalingId" to maalingId),
          ) { rs, _ ->
            rs.getString("uuid")
          })
          ?: throw NoSuchElementException("Fant ikkje testrunUuid for maalingId: $maalingId")
    }
  }

  fun getKontrollIdFromMaalingId(maalingId: Int): Int {

    return DataAccessUtils.singleResult(
        jdbcTemplate.query(
            """select kontrollId from "testlab2_testing"."maalingv1" where id = :maalingId""",
            mapOf("maalingId" to maalingId),
        ) { rs, _ ->
          rs.getInt("kontrollId")
        })
        ?: throw NoSuchElementException("Fant ikkje kontrollId for maalingId: $maalingId")
  }

  fun isMaalingFerdigTesta(maalingId: Int): Boolean {
    return jdbcTemplate
        .query(
            """select status from "testlab2_testing"."maalingv1" where id = :maalingId""",
            mapOf("maalingId" to maalingId)) { rs, _ ->
              val status = rs.getString("status")
              MaalingStatus.valueOf(status) == MaalingStatus.testing_ferdig
            }
        .first()
  }
}
