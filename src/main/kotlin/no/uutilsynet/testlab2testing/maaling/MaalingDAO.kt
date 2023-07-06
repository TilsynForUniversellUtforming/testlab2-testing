package no.uutilsynet.testlab2testing.maaling

import java.net.URI
import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loeysingRowMapper
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.maaling.Maaling.Crawling
import no.uutilsynet.testlab2testing.maaling.Maaling.Kvalitetssikring
import no.uutilsynet.testlab2testing.maaling.Maaling.Planlegging
import no.uutilsynet.testlab2testing.maaling.Maaling.Testing
import no.uutilsynet.testlab2testing.maaling.Maaling.TestingFerdig
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.crawlParametersRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.deleteMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingLoeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingByDateSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingByIdSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingByStatus
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.testResultatRowMapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.updateMaalingWithCrawlParameters
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.crawling
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.kvalitetssikring
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.planlegging
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.testing
import no.uutilsynet.testlab2testing.maaling.MaalingStatus.testing_ferdig
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
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
      val datoStart: LocalDate,
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
          URI(rs.getString("nettside")).toURL(),
          rs.getString("testregel"),
          rs.getInt("side_nivaa"),
          rs.getTimestamp("test_vart_utfoert").toLocalDateTime(),
          rs.getString("element_utfall"),
          rs.getString("element_resultat"),
          TestResultat.ACTElement(rs.getString("html_code"), rs.getString("pointer")))
    }

    val createMaalingSql =
        """
      insert into Maalingv1 (navn, status, dato_start, max_links_per_page, num_links_to_select, utval_id) 
      values (:navn, :status, :dato_start, :maxLinksPerPage, :numLinksToSelect, :utvalId)
      returning id
    """
            .trimIndent()

    fun createMaalingParams(
        navn: String,
        datoStart: LocalDate,
        crawlParameters: CrawlParameters,
        utvalId: UtvalId? = null
    ) =
        mapOf(
            "navn" to navn,
            "dato_start" to datoStart,
            "status" to "planlegging",
            "maxLinksPerPage" to crawlParameters.maxLinksPerPage,
            "numLinksToSelect" to crawlParameters.numLinksToSelect,
            "utvalId" to utvalId)

    val selectMaalingSql =
        "select id, navn, dato_start, status, max_links_per_page, num_links_to_select from Maalingv1"

    val selectMaalingByDateSql = "$selectMaalingSql order by dato_start desc"

    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val selectMaalingByStatus = "$selectMaalingSql where status in (:statusList)"

    val maalingLoeysingSql =
        """ 
      select l.id, l.namn, l.url, l.orgnummer
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
  }

  @Transactional
  fun createMaaling(
      navn: String,
      datoStart: LocalDate,
      utval: Utval,
      testregelIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Int {
    val idMaaling =
        jdbcTemplate.queryForObject(
            createMaalingSql,
            createMaalingParams(navn, datoStart, crawlParameters, utval.id),
            Int::class.java)!!
    val loeysingIdList = utval.loeysingar.map { it.id }
    for (idLoeysing: Int in loeysingIdList) {
      jdbcTemplate.update(
          "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)",
          mapOf("idMaaling" to idMaaling, "idLoeysing" to idLoeysing))
    }
    for (idTestregel: Int in testregelIdList) {
      jdbcTemplate.update(
          "insert into Maaling_Testregel (maaling_id, testregel_id) values (:maaling_id, :testregel_id)",
          mapOf("maaling_id" to idMaaling, "testregel_id" to idTestregel))
    }

    return idMaaling
  }

  @Transactional
  fun createMaaling(
      navn: String,
      datoStart: LocalDate,
      loyesingIds: List<Int>,
      testregelIdList: List<Int>,
      crawlParameters: CrawlParameters
  ): Int {
    val idMaaling =
        jdbcTemplate.queryForObject(
            createMaalingSql,
            createMaalingParams(navn, datoStart, crawlParameters),
            Int::class.java)!!
    for (idLoeysing: Int in loyesingIds) {
      jdbcTemplate.update(
          "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)",
          mapOf("idMaaling" to idMaaling, "idLoeysing" to idLoeysing))
    }
    for (idTestregel: Int in testregelIdList) {
      jdbcTemplate.update(
          "insert into Maaling_Testregel (maaling_id, testregel_id) values (:maaling_id, :testregel_id)",
          mapOf("maaling_id" to idMaaling, "testregel_id" to idTestregel))
    }

    return idMaaling
  }

  fun getMaaling(id: Int): Maaling? {
    val maaling =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(selectMaalingByIdSql, mapOf("id" to id), maalingRowmapper))

    return maaling?.toMaaling()
  }

  @Transactional
  fun deleteMaaling(id: Int): Int = jdbcTemplate.update(deleteMaalingSql, mapOf("id" to id))

  fun getMaalingList(): List<MaalingListElement> =
      jdbcTemplate
          .query(selectMaalingByDateSql, maalingRowmapper)
          .map { MaalingListElement(it.id, it.navn, it.datoStart, it.status.status) }
          .also { logger.debug("hentet ${it.size} målinger fra databasen") }

  fun getMaalingListByStatus(statusList: List<MaalingStatus>): List<Maaling> {
    return jdbcTemplate
        .query(
            selectMaalingByStatus,
            mapOf("statusList" to statusList.map { it.status }),
            maalingRowmapper)
        .map { it.toMaaling() }
        .also {
          logger.debug(
              "hentet ${it.size} målinger fra databasen med status 'crawling' eller 'testing'")
        }
  }

  private fun MaalingDTO.toMaaling(): Maaling {
    return when (status) {
      planlegging -> {
        val loeysingList =
            jdbcTemplate.query(maalingLoeysingSql, mapOf("id" to id), loeysingRowMapper)
        val testregelList =
            jdbcTemplate.query(maalingTestregelSql, mapOf("id" to id), testregelRowMapper)
        Planlegging(
            id,
            navn,
            datoStart,
            loeysingList,
            testregelList,
            CrawlParameters(maxLinksPerPage, numLinksToSelect))
      }
      crawling,
      kvalitetssikring -> {
        val crawlResultat = getCrawlResultatForMaaling(id)
        if (status == crawling) {
          Crawling(this.id, this.navn, this.datoStart, crawlResultat)
        } else {
          Kvalitetssikring(id, navn, datoStart, crawlResultat)
        }
      }
      testing,
      testing_ferdig -> {
        val testKoeyringar = getTestKoeyringarForMaaling(id)
        if (status == testing) {
          Testing(id, navn, datoStart, testKoeyringar)
        } else {
          TestingFerdig(id, navn, datoStart, testKoeyringar)
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
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side
              from testkoeyring t
              join loeysing l on l.id = t.loeysing_id
              where maaling_id = :maaling_id
            """
            .trimIndent(),
        mapOf("maaling_id" to maalingId),
        fun(rs: ResultSet, _: Int): TestKoeyring {
          val status = rs.getString("status")
          val loeysingId = rs.getInt("loeysing_id")
          val crawlResultatForLoeysing =
              crawlResultat.find { it.loeysing.id == loeysingId }
                  ?: throw RuntimeException(
                      "finner ikkje crawlresultat for loeysing med id = $loeysingId")
          if (crawlResultatForLoeysing !is CrawlResultat.Ferdig) {
            throw RuntimeException(
                "crawlresultat for loeysing med id = $loeysingId er ikkje ferdig")
          }

          val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
          return when (status) {
            "ikkje_starta" -> {
              TestKoeyring.IkkjeStarta(
                  crawlResultatForLoeysing, sistOppdatert, URI(rs.getString("status_url")).toURL())
            }
            "starta" -> {
              TestKoeyring.Starta(
                  crawlResultatForLoeysing,
                  sistOppdatert,
                  URI(rs.getString("status_url")).toURL(),
                  Framgang(rs.getInt("lenker_testa"), crawlResultatForLoeysing.nettsider.size))
            }
            "feila" ->
                TestKoeyring.Feila(
                    crawlResultatForLoeysing, sistOppdatert, rs.getString("feilmelding"))
            "ferdig" -> {
              val testkoeyringId = rs.getInt("id")
              val testResultat: List<TestResultat> = getTestResultat(testkoeyringId)
              val urlFulltResultat = rs.getString("url_fullt_resultat")
              val urlBrot = rs.getString("url_brot")
              val urlAggTR = rs.getString("url_agg_tr")
              val urlAggSK = rs.getString("url_agg_sk")
              val urlAggSide = rs.getString("url_agg_side")
              val lenker =
                  if (urlFulltResultat != null)
                      AutoTesterClient.AutoTesterOutput.Lenker(
                          URL(urlFulltResultat),
                          URL(urlBrot),
                          URL(urlAggTR),
                          URL(urlAggSK),
                          URL(urlAggSide))
                  else null
              TestKoeyring.Ferdig(
                  crawlResultatForLoeysing,
                  sistOppdatert,
                  URI(rs.getString("status_url")).toURL(),
                  testResultat,
                  lenker)
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
                       loeysing.orgnummer,
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
    val loeysing =
        Loeysing(
            rs.getInt("lid"),
            rs.getString("namn"),
            URI(rs.getString("url")).toURL(),
            rs.getString("orgnummer"))
    val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
    val status = rs.getString("status")
    val id = rs.getInt("crid")

    val crawlResultat =
        when (status) {
          "ikke_ferdig" -> {
            val framgang = Framgang(rs.getInt("lenker_crawla"), rs.getInt("max_links_per_page"))
            CrawlResultat.IkkeFerdig(
                URI(rs.getString("status_url")).toURL(), loeysing, sistOppdatert, framgang)
          }
          "feilet" -> {
            var statusUrl: URL? = null
            val statusUrlDB = rs.getString("status_url")
            if (statusUrlDB != null) {
              statusUrl = URL(statusUrlDB)
            }

            CrawlResultat.Feilet(rs.getString("feilmelding"), statusUrl, loeysing, sistOppdatert)
          }
          "ferdig" -> {
            val nettsider = mutableListOf<URL>()
            val statusUrl = rs.getString("status_url")

            while (isSameCrawlResultat(rs, id)) {
              val nettside = rs.getString("nettside_url")
              if (nettside != null) {
                nettsider.add(URI(nettside).toURL())
              } else {
                logger.warn("nettside mangler for crawlresultat $id med status `ferdig`.")
              }
              rs.next()
            }

            val nettsideList = nettsider.toList()
            CrawlResultat.Ferdig(nettsideList, URI(statusUrl).toURL(), loeysing, sistOppdatert)
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

      val deleteParams = mapOf("maalingId" to maaling.id)

      jdbcTemplate.update("delete from MaalingLoeysing where idMaaling = :maalingId", deleteParams)
      val updateBatchValuesLoeysing =
          maaling.loeysingList.map { mapOf("maalingId" to maaling.id, "loeysingId" to it.id) }
      jdbcTemplate.batchUpdate(
          "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:maalingId, :loeysingId)",
          updateBatchValuesLoeysing.toTypedArray())

      jdbcTemplate.update(
          "delete from Maaling_Testregel where maaling_id = :maalingId", deleteParams)
      val updateBatchValuesTestregel =
          maaling.testregelList.map { mapOf("maaling_id" to maaling.id, "testregel_id" to it.id) }
      jdbcTemplate.batchUpdate(
          "insert into Maaling_Testregel (maaling_id, testregel_id) values (:maaling_id, :testregel_id)",
          updateBatchValuesTestregel.toTypedArray())
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
        when (testKoeyring) {
          is TestKoeyring.Starta -> {
            jdbcTemplate.queryForObject(
                """insert into testkoeyring (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, lenker_testa) 
                values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding, :lenker_testa)
                returning id
            """
                    .trimMargin(),
                mapOf(
                    "maaling_id" to maalingId,
                    "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
                    "status" to status(testKoeyring),
                    "status_url" to statusURL(testKoeyring),
                    "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
                    "feilmelding" to feilmelding(testKoeyring),
                    "lenker_testa" to testKoeyring.framgang.prosessert),
                Int::class.java)
          }
          is TestKoeyring.Ferdig -> {
            jdbcTemplate.queryForObject(
                """
              insert into testkoeyring(maaling_id, loeysing_id, status, status_url, sist_oppdatert, url_fullt_resultat, url_brot, url_agg_tr, url_agg_sk,url_agg_side)
              values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :url_fullt_resultat, :url_brot, :url_agg_tr, :url_agg_sk, :url_agg_side)
              returning id
            """
                    .trimIndent(),
                mapOf(
                    "maaling_id" to maalingId,
                    "loeysing_id" to testKoeyring.crawlResultat.loeysing.id,
                    "status" to status(testKoeyring),
                    "status_url" to statusURL(testKoeyring),
                    "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
                    "url_fullt_resultat" to testKoeyring.lenker?.urlFulltResultat?.toString(),
                    "url_brot" to testKoeyring.lenker?.urlBrot?.toString(),
                    "url_agg_tr" to testKoeyring.lenker?.urlAggregeringTR?.toString(),
                    "url_agg_sk" to testKoeyring.lenker?.urlAggregeringSK?.toString(),
                    "url_agg_side" to testKoeyring.lenker?.urlAggregeringSide?.toString()),
                Int::class.java)
          }
          else -> {
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
          }
        }
    if (testKoeyring is TestKoeyring.Ferdig) {
      // DFK-216 OK. Lagres bare for målinger som inneholder testResultat. Kan fjernes når vi
      // fjerner testResultat fra TestKoeyring.
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
            "pointer" to testResultat.elementOmtale?.pointer,
            "html_code" to testResultat.elementOmtale?.htmlCode))
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
                "lenker_crawla" to crawlResultat.framgang.prosessert))
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
