package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loysingRowmapper
import no.uutilsynet.testlab2testing.maaling.Maaling.Crawling
import no.uutilsynet.testlab2testing.maaling.Maaling.Kvalitetssikring
import no.uutilsynet.testlab2testing.maaling.Maaling.Planlegging
import no.uutilsynet.testlab2testing.maaling.Maaling.Testing
import no.uutilsynet.testlab2testing.maaling.Maaling.TestingFerdig
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.crawlParametersRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingLoysingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingLoysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.deleteMaalingLoeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.deleteMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingLoeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingByIdSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectTestResultForMaalingLoeysingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectTestResultForMaalingLoeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.testResultatRowMapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingWithCrawlParameters
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.crawling
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.kvalitetssikring
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.planlegging
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.testing
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.testing_ferdig
import org.slf4j.LoggerFactory
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MaalingDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  private val logger = LoggerFactory.getLogger(MaalingDAO::class.java)

  data class MaalingDTO(
      val id: Int,
      val navn: String,
      val status: MaalingStatus,
      val maxLinksPerPage: Int,
      val numLinksToSelect: Int
  )

  object MaalingParams {
    val maalingRowmapper = DataClassRowMapper.newInstance(MaalingDTO::class.java)
    val crawlParametersRowmapper = DataClassRowMapper.newInstance(CrawlParameters::class.java)
    val testResultatRowMapper: (ResultSet, Int) -> TestResultat = { rs: ResultSet, _: Int ->
      TestResultat(
          rs.getString("suksesskriterium").split(","),
          URL(rs.getString("nettside")),
          rs.getString("testregel"),
          rs.getInt("side_nivaa"),
          rs.getTimestamp("test_vart_utfoert").toLocalDateTime(),
          rs.getString("element_utfall"),
          rs.getString("element_resultat"),
          TestResultat.ACTElement(rs.getString("html_code"), rs.getString("pointer")))
    }

    val createMaalingSql =
        """
      insert into Maalingv1 (navn, status, max_links_per_page, num_links_to_select) 
      values (:navn, :status, :maxLinksPerPage, :numLinksToSelect)
    """
            .trimIndent()
    fun createMaalingParams(navn: String, crawlParameters: CrawlParameters) =
        MapSqlParameterSource("navn", navn)
            .addValue("status", "planlegging")
            .addValue("maxLinksPerPage", crawlParameters.maxLinksPerPage)
            .addValue("numLinksToSelect", crawlParameters.numLinksToSelect)

    val deleteMaalingLoeysingSql = "delete from MaalingLoeysing where idMaaling = :idMaaling"

    val createMaalingLoysingSql =
        "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"
    fun createMaalingLoysingParams(idMaaling: Int, idLoeysing: Int) =
        MapSqlParameterSource("idMaaling", idMaaling).addValue("idLoeysing", idLoeysing)

    val selectMaalingSql =
        "select id, navn, status, max_links_per_page, num_links_to_select from Maalingv1"
    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val maalingLoeysingSql =
        """ 
      select l.id, l.namn, l.url 
      from MaalingLoeysing ml 
        join loeysing l on ml.idLoeysing = l.id
      where ml.idMaaling = :id
      """
            .trimIndent()

    val updateMaalingSql = "update MaalingV1 set navn = :navn, status = :status where id = :id"
    fun updateMaalingParams(maaling: Maaling): Map<String, Any> {
      val status =
          when (maaling) {
            is Planlegging -> "planlegging"
            is Crawling -> "crawling"
            is Kvalitetssikring -> "kvalitetssikring"
            is Testing -> "testing"
            is TestingFerdig -> "testing_ferdig"
          }
      return mapOf("navn" to maaling.navn, "status" to status, "id" to maaling.id)
    }
    val updateMaalingWithCrawlParameters =
        "update MaalingV1 set navn = :navn, status = :status, max_links_per_page = :maxLinksPerPage, num_links_to_select = :numLinksToSelect where id = :id"

    val deleteMaalingSql = "delete from MaalingV1 where id = :id"

    val selectTestResultForMaalingLoeysingSql =
        """
          select nettside, suksesskriterium, testregel, element_utfall, element_resultat, side_nivaa, test_vart_utfoert, pointer, html_code
            from testresultat tr
            join testkoeyring t on tr.testkoeyring_id = t.id
          where t.maaling_id = :maalingId and t.loeysing_id = :loeysingId
        """
            .trimIndent()

    fun selectTestResultForMaalingLoeysingParams(maalingId: Int, loeysingId: Int) =
        MapSqlParameterSource("maalingId", maalingId).addValue("loeysingId", loeysingId)
  }

  @Transactional
  fun createMaaling(navn: String, loyesingIds: List<Int>, crawlParameters: CrawlParameters): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()
    jdbcTemplate.update(
        createMaalingSql, createMaalingParams(navn, crawlParameters), keyHolder, arrayOf("id"))
    val idMaaling = keyHolder.key!!.toInt()

    for (idLoysing: Int in loyesingIds) {
      jdbcTemplate.update(createMaalingLoysingSql, createMaalingLoysingParams(idMaaling, idLoysing))
    }

    return keyHolder.key!!.toInt()
  }

  fun getMaaling(id: Int): Maaling? {
    val maaling =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                selectMaalingByIdSql, MapSqlParameterSource("id", id), maalingRowmapper))

    return maaling?.toMaaling()
  }

  fun getTestresultatForMaalingLoeysing(maalingId: Int, loeysingId: Int): List<TestResultat> =
      jdbcTemplate.query(
          selectTestResultForMaalingLoeysingSql,
          selectTestResultForMaalingLoeysingParams(maalingId, loeysingId),
          testResultatRowMapper)

  @Transactional
  fun deleteMaaling(id: Int): Int =
      jdbcTemplate.update(deleteMaalingSql, MapSqlParameterSource("id", id))

  fun getMaalingList(): List<Maaling> =
      jdbcTemplate
          .query(selectMaalingSql, maalingRowmapper)
          .map { it.toMaaling() }
          .also { logger.info("hentet ${it.size} målinger fra databasen") }

  private fun MaalingDTO.toMaaling(): Maaling =
      when (status) {
        planlegging -> {
          val loeysingList =
              jdbcTemplate.query(
                  maalingLoeysingSql, MapSqlParameterSource("id", id), loysingRowmapper)
          Planlegging(id, navn, loeysingList, CrawlParameters(maxLinksPerPage, numLinksToSelect))
        }
        crawling,
        kvalitetssikring -> {
          val crawlResultat =
              jdbcTemplate.query(
                  """
                    select cr.id as crid, cr.status, cr.status_url, cr.sist_oppdatert, cr.feilmelding,
                    l.id as lid, l.namn, l.url,
                    n.url as nettside_url
                    from crawlresultat cr
                    join loeysing l on cr.loeysingid = l.id
                    left join nettside n on cr.id = n.crawlresultat_id
                    where maaling_id = :maalingId
                  """
                      .trimIndent(),
                  mapOf("maalingId" to this.id),
                  fun(rs: ResultSet): List<CrawlResultat> {
                    val result = mutableListOf<CrawlResultat>()
                    rs.next()

                    while (!rs.isAfterLast) {
                      result.add(toCrawlResultat(rs))
                    }

                    return result.toList()
                  })
                  ?: throw RuntimeException(
                      "fikk `null` da vi forsøkte å hente crawlresultat for måling med id = $id")
          if (status == crawling) {
            Crawling(this.id, this.navn, crawlResultat)
          } else {
            Kvalitetssikring(id, navn, crawlResultat)
          }
        }
        testing,
        testing_ferdig -> {
          val testKoeyringar =
              jdbcTemplate.query(
                  """
                select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, l.namn as loeysing_namn, l.url as loeysing_url
                from testkoeyring t
                join loeysing l on l.id = t.loeysing_id
                where maaling_id = :maaling_id
              """
                      .trimIndent(),
                  mapOf("maaling_id" to id),
                  fun(rs: ResultSet, _: Int): TestKoeyring {
                    val status = rs.getString("status")
                    val loeysing =
                        Loeysing(
                            rs.getInt("loeysing_id"),
                            rs.getString("loeysing_namn"),
                            URL(rs.getString("loeysing_url")))
                    val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
                    return when (status) {
                      "ikkje_starta" -> {
                        TestKoeyring.IkkjeStarta(
                            loeysing, sistOppdatert, URL(rs.getString("status_url")))
                      }
                      "starta" -> {
                        TestKoeyring.Starta(
                            loeysing, sistOppdatert, URL(rs.getString("status_url")))
                      }
                      "feila" ->
                          TestKoeyring.Feila(loeysing, sistOppdatert, rs.getString("feilmelding"))
                      "ferdig" -> {
                        val testkoeyringId = rs.getInt("id")
                        val testResultat: List<TestResultat> = getTestResultat(testkoeyringId)
                        TestKoeyring.Ferdig(
                            loeysing, sistOppdatert, URL(rs.getString("status_url")), testResultat)
                      }
                      else -> throw RuntimeException("ukjent status $status")
                    }
                  })
          if (status == testing) {
            Testing(id, navn, testKoeyringar)
          } else {
            val ferdigeTestKoeyringar = testKoeyringar.filterIsInstance<TestKoeyring.Ferdig>()
            TestingFerdig(id, navn, ferdigeTestKoeyringar)
          }
        }
      }

  private fun getTestResultat(testkoeyringId: Int): List<TestResultat> {
    return jdbcTemplate.query(
        """
          select nettside, suksesskriterium, testregel, element_utfall, element_resultat, side_nivaa, test_vart_utfoert, pointer, html_code
          from testresultat
          where testkoeyring_id = :testkoeyring_id
        """
            .trimIndent(),
        mapOf("testkoeyring_id" to testkoeyringId),
        testResultatRowMapper)
  }

  fun getCrawlParameters(maalingId: Int): CrawlParameters =
      runCatching {
            jdbcTemplate.queryForObject(
                "select m.max_links_per_page, m.num_links_to_select from maalingv1 m where m.id = :id",
                mapOf("id" to maalingId),
                crawlParametersRowmapper)
                ?: throw RuntimeException("Fant ikke crawlparametere for maaling $maalingId")
          }
          .getOrElse {
            logger.error(
                "Kunne ikke hente crawlparametere for maaling $maalingId, velger default parametere")
            throw it
          }

  private fun toCrawlResultat(rs: ResultSet): CrawlResultat {
    val loeysing = Loeysing(rs.getInt("lid"), rs.getString("namn"), URL(rs.getString("url")))
    val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
    val status = rs.getString("status")
    val id = rs.getInt("crid")

    val crawlResultat =
        when (status) {
          "ikke_ferdig" -> {
            CrawlResultat.IkkeFerdig(URL(rs.getString("status_url")), loeysing, sistOppdatert)
          }
          "feilet" -> {
            CrawlResultat.Feilet(rs.getString("feilmelding"), loeysing, sistOppdatert)
          }
          "ferdig" -> {
            val nettsider = mutableListOf<URL>()
            val statusUrl = rs.getString("status_url")

            while (isSameCrawlResultat(rs, id)) {
              val nettside = rs.getString("nettside_url")
              if (nettside != null) {
                nettsider.add(URL(nettside))
              } else {
                logger.warn("nettside mangler for crawlresultat $id med status `ferdig`.")
              }
              rs.next()
            }
            CrawlResultat.Ferdig(nettsider.toList(), URL(statusUrl), loeysing, sistOppdatert)
          }
          else -> throw RuntimeException("ukjent status lagret i databasen: $status")
        }

    if (isSameCrawlResultat(rs, id)) {
      rs.next()
    }

    return crawlResultat
  }

  private fun isSameCrawlResultat(rs: ResultSet, id: Int) =
      !rs.isAfterLast && rs.getInt("crid") == id

  @Transactional
  fun updateMaaling(maaling: Maaling) {
    if (maaling is Planlegging) {
      jdbcTemplate.update(
          updateMaalingWithCrawlParameters,
          mapOf(
              "id" to maaling.id,
              "navn" to maaling.navn,
              "status" to "planlegging",
              "maxLinksPerPage" to maaling.crawlParameters.maxLinksPerPage,
              "numLinksToSelect" to maaling.crawlParameters.numLinksToSelect))
      jdbcTemplate.update(deleteMaalingLoeysingSql, MapSqlParameterSource("idMaaling", maaling.id))
      val updateBatchValues =
          maaling.loeysingList.map {
            MapSqlParameterSource("idMaaling", maaling.id).addValue("idLoeysing", it.id)
          }
      jdbcTemplate.batchUpdate(createMaalingLoysingSql, updateBatchValues.toTypedArray())
    } else {
      jdbcTemplate.update(updateMaalingSql, updateMaalingParams(maaling))
    }
  }

  @Transactional
  fun save(maaling: Maaling): Result<Maaling> = runCatching {
    updateMaaling(maaling)
    when (maaling) {
      is Planlegging -> {}
      is Crawling -> {
        maaling.crawlResultat.forEach { saveCrawlResultat(it, maaling) }
      }
      is Kvalitetssikring -> {
        maaling.crawlResultat.forEach { saveCrawlResultat(it, maaling) }
      }
      is Testing -> {
        maaling.testKoeyringar.forEach { saveTestKoeyring(it, maaling.id) }
      }
      is TestingFerdig -> {
        maaling.testKoeyringar.forEach { saveTestKoeyring(it, maaling.id) }
      }
    }
    maaling
  }

  @Transactional
  fun saveTestKoeyring(testKoeyring: TestKoeyring, maalingId: Int) {
    jdbcTemplate.update(
        """delete from testkoeyring where maaling_id = :maaling_id and loeysing_id = :loeysing_id""",
        mapOf("maaling_id" to maalingId, "loeysing_id" to testKoeyring.loeysing.id))
    val testKoeyringId =
        jdbcTemplate.queryForObject(
            """insert into testkoeyring (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding) 
                values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding)
                returning id
            """
                .trimMargin(),
            mapOf(
                "maaling_id" to maalingId,
                "loeysing_id" to testKoeyring.loeysing.id,
                "status" to status(testKoeyring),
                "status_url" to statusURL(testKoeyring),
                "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
                "feilmelding" to feilmelding(testKoeyring)),
            Int::class.java)
    if (testKoeyring is TestKoeyring.Ferdig) {
      testKoeyring.testResultat.forEach { saveTestResultat(testKoeyringId!!, it) }
    }
  }

  private fun saveTestResultat(testkoeyringId: Int, testResultat: TestResultat) {
    jdbcTemplate.update(
        """
         insert into testresultat (testkoeyring_id, nettside, suksesskriterium, testregel, element_utfall, element_resultat, side_nivaa, test_vart_utfoert, pointer, html_code)
         values (:testkoeyring_id, :nettside, :suksesskriterium, :testregel, :element_utfall, :element_resultat, :side_nivaa, :test_vart_utfoert, :pointer, :html_code)
            """
            .trimIndent(),
        mapOf(
            "testkoeyring_id" to testkoeyringId,
            "nettside" to testResultat.side.toString(),
            "suksesskriterium" to testResultat.suksesskriterium.joinToString(separator = ","),
            "testregel" to testResultat.testregelId,
            "element_utfall" to testResultat.elementUtfall,
            "element_resultat" to testResultat.elementResultat,
            "side_nivaa" to testResultat.sideNivaa,
            "test_vart_utfoert" to Timestamp.valueOf(testResultat.testVartUtfoert),
            "pointer" to testResultat.elementOmtale.pointer,
            "html_code" to testResultat.elementOmtale.htmlCode))
  }

  private fun status(testKoeyring: TestKoeyring): String =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> "ikkje_starta"
        is TestKoeyring.Starta -> "starta"
        is TestKoeyring.Feila -> "feila"
        is TestKoeyring.Ferdig -> "ferdig"
      }

  private fun feilmelding(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.Feila -> testKoeyring.feilmelding
        else -> null
      }

  private fun statusURL(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Starta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Ferdig -> testKoeyring.statusURL.toString()
        else -> null
      }

  @Transactional
  fun saveCrawlResultat(crawlResultat: CrawlResultat, maaling: Maaling) {
    jdbcTemplate.update(
        "delete from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
        mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maaling.id))
    when (crawlResultat) {
      is CrawlResultat.IkkeFerdig -> {
        jdbcTemplate.update(
            "insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)",
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to "ikke_ferdig",
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maaling.id,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert)))
      }
      is CrawlResultat.Feilet -> {
        jdbcTemplate.update(
            """
              insert into crawlresultat (loeysingid, status, maaling_id, sist_oppdatert, feilmelding)
              values (:loeysingid, 'feilet', :maaling_id, :sist_oppdatert, :feilmelding)
            """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "maaling_id" to maaling.id,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "feilmelding" to crawlResultat.feilmelding))
      }
      is CrawlResultat.Ferdig -> {
        jdbcTemplate.update(
            "insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)",
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to "ferdig",
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maaling.id,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert)))
        val id =
            jdbcTemplate.queryForObject(
                "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
                mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maaling.id),
                Int::class.java)
        crawlResultat.nettsider.forEach { nettside ->
          jdbcTemplate.update(
              "insert into nettside (crawlresultat_id, url) values (:cr_id, :url)",
              mapOf("cr_id" to id, "url" to nettside.toString()))
        }
      }
    }
  }
}
