package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loysingRowmapper
import no.uutilsynet.testlab2testing.maaling.Maaling.*
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
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.*
import org.slf4j.LoggerFactory
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
      returning id
    """
            .trimIndent()
    fun createMaalingParams(navn: String, crawlParameters: CrawlParameters) =
        mapOf(
            "navn" to navn,
            "status" to "planlegging",
            "maxLinksPerPage" to crawlParameters.maxLinksPerPage,
            "numLinksToSelect" to crawlParameters.numLinksToSelect)

    val deleteMaalingLoeysingSql = "delete from MaalingLoeysing where idMaaling = :idMaaling"

    val createMaalingLoysingSql =
        "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"
    fun createMaalingLoysingParams(idMaaling: Int, idLoeysing: Int) =
        mapOf("idMaaling" to idMaaling, "idLoeysing" to idLoeysing)

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
        mapOf("maalingId" to maalingId, "loeysingId" to loeysingId)
  }

  @Transactional
  fun createMaaling(navn: String, loyesingIds: List<Int>, crawlParameters: CrawlParameters): Int {
    val idMaaling =
        jdbcTemplate.queryForObject(
            createMaalingSql, createMaalingParams(navn, crawlParameters), Int::class.java)!!
    for (idLoysing: Int in loyesingIds) {
      jdbcTemplate.update(createMaalingLoysingSql, createMaalingLoysingParams(idMaaling, idLoysing))
    }

    return idMaaling
  }

  fun getMaaling(id: Int): Maaling? {
    val maaling =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(selectMaalingByIdSql, mapOf("id" to id), maalingRowmapper))

    return maaling?.toMaaling()
  }

  fun getTestresultatForMaalingLoeysing(maalingId: Int, loeysingId: Int): List<TestResultat> =
      jdbcTemplate.query(
          selectTestResultForMaalingLoeysingSql,
          selectTestResultForMaalingLoeysingParams(maalingId, loeysingId),
          testResultatRowMapper)

  @Transactional
  fun deleteMaaling(id: Int): Int = jdbcTemplate.update(deleteMaalingSql, mapOf("id" to id))

  fun getMaalingList(): List<Maaling> =
      jdbcTemplate
          .query(selectMaalingSql, maalingRowmapper)
          .map { it.toMaaling() }
          .also { logger.debug("hentet ${it.size} målinger fra databasen") }

  private fun MaalingDTO.toMaaling(): Maaling {
    return when (status) {
      planlegging -> {
        val loeysingList =
            jdbcTemplate.query(maalingLoeysingSql, mapOf("id" to id), loysingRowmapper)
        Planlegging(id, navn, loeysingList, CrawlParameters(maxLinksPerPage, numLinksToSelect))
      }
      crawling,
      kvalitetssikring -> {
        val crawlResultat = getCrawlResultatForMaaling(id)
        if (status == crawling) {
          Crawling(this.id, this.navn, crawlResultat)
        } else {
          Kvalitetssikring(id, navn, crawlResultat)
        }
      }
      testing,
      testing_ferdig -> {
        val testKoeyringar = getTestKoeyringarForMaaling(id)
        if (status == testing) {
          Testing(id, navn, testKoeyringar)
        } else {
          TestingFerdig(id, navn, testKoeyringar)
        }
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

  private fun getTestKoeyringarForMaaling(maalingId: Int): List<TestKoeyring> {
    val crawlResultat = getCrawlResultatForMaaling(maalingId)
    return jdbcTemplate.query<TestKoeyring>(
        """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding
              from testkoeyring t
              join loeysing l on l.id = t.loeysing_id
              where maaling_id = :maaling_id
            """
            .trimIndent(),
        mapOf<String?, Int>("maaling_id" to maalingId),
        fun(rs: ResultSet, _: Int): TestKoeyring {
          val status = rs.getString("status")
          val loeysingId = rs.getInt("loeysing_id")
          val crawlResultatForLoeysing =
              crawlResultat.find { it.loeysing.id == loeysingId }
                  ?: throw RuntimeException(
                      "finner ikkje crawlresultat for loeysing med id = $loeysingId")
          val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
          return when (status) {
            "ikkje_starta" -> {
              TestKoeyring.IkkjeStarta(
                  crawlResultatForLoeysing, sistOppdatert, URL(rs.getString("status_url")))
            }
            "starta" -> {
              TestKoeyring.Starta(
                  crawlResultatForLoeysing, sistOppdatert, URL(rs.getString("status_url")))
            }
            "feila" ->
                TestKoeyring.Feila(
                    crawlResultatForLoeysing, sistOppdatert, rs.getString("feilmelding"))
            "ferdig" -> {
              val testkoeyringId = rs.getInt("id")
              val testResultat: List<TestResultat> = getTestResultat(testkoeyringId)
              TestKoeyring.Ferdig(
                  crawlResultatForLoeysing,
                  sistOppdatert,
                  URL(rs.getString("status_url")),
                  testResultat)
            }
            else -> throw RuntimeException("ukjent status $status")
          }
        })
  }

  private fun getCrawlResultatForMaaling(maalingId: Int) =
      jdbcTemplate.query(
          """
                select crawlresultat.id as crid,
                       crawlresultat.status,
                       crawlresultat.status_url,
                       crawlresultat.sist_oppdatert,
                       crawlresultat.feilmelding,
                       crawlresultat.lenker_crawla, loeysing.id as lid, loeysing.namn,
                       loeysing.url,
                       nettside.url as nettside_url,
                       maaling.max_links_per_page
                from crawlresultat
                         join loeysing on crawlresultat.loeysingid = loeysing.id
                         left join nettside on crawlresultat.id = nettside.crawlresultat_id
                         join maalingv1 maaling on maaling.id = crawlresultat.maaling_id
                where crawlresultat.maaling_id = :maalingId
                order by maaling.id, loeysing.id
              """
              .trimIndent(),
          mapOf("maalingId" to maalingId),
          fun(rs: ResultSet): List<CrawlResultat> {
            val result = mutableListOf<CrawlResultat>()
            rs.next()

            while (!rs.isAfterLast) {
              result.add(toCrawlResultat(rs))
            }

            return result.toList()
          })
          ?: throw RuntimeException(
              "fikk `null` da vi forsøkte å hente crawlresultat for måling med id = $maalingId")

  private fun toCrawlResultat(rs: ResultSet): CrawlResultat {
    val loeysing = Loeysing(rs.getInt("lid"), rs.getString("namn"), URL(rs.getString("url")))
    val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
    val status = rs.getString("status")
    val id = rs.getInt("crid")

    val crawlResultat =
        when (status) {
          "ikke_ferdig" -> {
            val framgang =
                CrawlResultat.Framgang(rs.getInt("lenker_crawla"), rs.getInt("max_links_per_page"))
            CrawlResultat.IkkeFerdig(
                URL(rs.getString("status_url")), loeysing, sistOppdatert, framgang)
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
      jdbcTemplate.update(deleteMaalingLoeysingSql, mapOf("idMaaling" to maaling.id))
      val updateBatchValues =
          maaling.loeysingList.map { mapOf("idMaaling" to maaling.id, "idLoeysing" to it.id) }
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
        maaling.crawlResultat.forEach { saveCrawlResultat(it, maaling.id) }
      }
      is Kvalitetssikring -> {
        maaling.crawlResultat.forEach { saveCrawlResultat(it, maaling.id) }
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
    saveCrawlResultat(testKoeyring.crawlResultat, maalingId)
    jdbcTemplate.update(
        """delete from testkoeyring where maaling_id = :maaling_id and loeysing_id = :loeysing_id""",
        mapOf("maaling_id" to maalingId, "loeysing_id" to testKoeyring.crawlResultat.loeysing.id))
    val testKoeyringId =
        jdbcTemplate.queryForObject(
            """insert into testkoeyring (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding) 
                values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding)
                returning id
            """
                .trimMargin(),
            mapOf(
                "maaling_id" to maalingId,
                "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
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
  fun saveCrawlResultat(crawlResultat: CrawlResultat, maalingId: Int) {
    jdbcTemplate.update(
        "delete from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
        mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maalingId))
    when (crawlResultat) {
      is CrawlResultat.IkkeFerdig -> {
        jdbcTemplate.update(
            """
              insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert, lenker_crawla) 
              values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert, :lenker_crawla)
            """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to "ikke_ferdig",
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "lenker_crawla" to crawlResultat.framgang.lenkerCrawla))
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
                "maaling_id" to maalingId,
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
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert)))
        val id =
            jdbcTemplate.queryForObject(
                "select id from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
                mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maalingId),
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
