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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
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
      val maxLenker: Int,
      val talLenker: Int
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
      insert into Maalingv1 (navn, status, dato_start, max_lenker, tal_lenker, utval_id) 
      values (:navn, :status, :dato_start, :max_lenker, :tal_lenker, :utvalId)
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
            "max_lenker" to crawlParameters.maxLenker,
            "tal_lenker" to crawlParameters.talLenker,
            "utvalId" to utvalId)

    val selectMaalingSql =
        "select id, navn, dato_start, status, max_lenker, tal_lenker from Maalingv1"

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
            id, navn, datoStart, loeysingList, testregelList, CrawlParameters(maxLenker, talLenker))
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
                "select m.max_lenker, m.tal_lenker from maalingv1 m where m.id = :id",
                mapOf("id" to maalingId),
                crawlParametersRowmapper)
                ?: throw RuntimeException("Fant ikke crawlparametere for maaling $maalingId")
          }
          .getOrElse {
            logger.error(
                "Kunne ikke hente crawlparametere for maaling $maalingId, velger default parametere")
            throw it
          }

  fun getCrawlResultatNettsider(maalingId: Int, loeysingId: Int): List<URL> =
      jdbcTemplate
          .queryForList(
              """
                select n.url
                from nettside n
                    join crawlresultat cr on n.crawlresultat_id = cr.id
                where cr.maaling_id = :maalingId
                    and cr.loeysingid = :loeysingId
              """
                  .trimIndent(),
              mapOf("maalingId" to maalingId, "loeysingId" to loeysingId),
              String::class.java)
          .map { url -> URI(url).toURL() }

  private fun getTestKoeyringarForMaaling(maalingId: Int): List<TestKoeyring> {
    val crawlResultat = getCrawlResultatForMaaling(maalingId)
    return jdbcTemplate.query<TestKoeyring>(
        """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side,url_agg_side_tr,url_agg_loeysing
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
                  Framgang(rs.getInt("lenker_testa"), crawlResultatForLoeysing.antallNettsider))
            }
            "feila" ->
                TestKoeyring.Feila(
                    crawlResultatForLoeysing, sistOppdatert, rs.getString("feilmelding"))
            "ferdig" -> {
              val testkoeyringId = rs.getInt("id")
              val testResultat: List<TestResultat> = getTestResultat(testkoeyringId)
              val urlFulltResultat = resultSetToURL(rs, "url_fullt_resultat")
              val urlBrot = resultSetToURL(rs, "url_brot")
              val urlAggTR = resultSetToURL(rs, "url_agg_tr")
              val urlAggSK = resultSetToURL(rs, "url_agg_sk")
              val urlAggSide = resultSetToURL(rs, "url_agg_side")
              val urlAggSideTR = resultSetToURL(rs, "url_agg_side_tr")
              val urlAggLoeysing = resultSetToURL(rs, "url_agg_loeysing")

              val lenker =
                  AutoTesterClient.AutoTesterOutput.Lenker(
                      urlFulltResultat,
                      urlBrot,
                      urlAggTR,
                      urlAggSK,
                      urlAggSide,
                      urlAggSideTR,
                      urlAggLoeysing)
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

  private fun resultSetToURL(rs: ResultSet, field: String): URL {
    return URI(rs.getString(field)).toURL()
  }

  fun getCrawlResultatForMaaling(maalingId: Int) =
      jdbcTemplate.query(
          """
          with agg_nettsider as (
              select crawlresultat_id, count(*) as ant_nettsider
              from nettside
              group by crawlresultat_id
          )
          select
              cr.id as crid,
              cr.status,
              cr.status_url,
              cr.sist_oppdatert,
              cr.feilmelding,
              cr.lenker_crawla,
              l.id as lid,
              l.namn,
              l.url,
              l.orgnummer,
              coalesce(an.ant_nettsider, 0) as ant_nettsider,
              m.max_lenker
          from crawlresultat cr
              join loeysing l on cr.loeysingid = l.id
              left join agg_nettsider an on cr.id = an.crawlresultat_id
              join maalingv1 m on m.id = cr.maaling_id
          where
              cr.maaling_id = :maalingId
          order by m.id, l.id
              """
              .trimIndent(),
          mapOf("maalingId" to maalingId),
          fun(rs: ResultSet): List<CrawlResultat> {
            val result = mutableListOf<CrawlResultat>()

            while (rs.next()) {
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

    val crawlResultat =
        when (status) {
          "ikkje_starta" -> {
            CrawlResultat.IkkjeStarta(
                URI(rs.getString("status_url")).toURL(), loeysing, sistOppdatert)
          }
          "starta" -> {
            val framgang = Framgang(rs.getInt("lenker_crawla"), rs.getInt("max_lenker"))
            CrawlResultat.Starta(
                URI(rs.getString("status_url")).toURL(), loeysing, sistOppdatert, framgang)
          }
          "feila" -> {
            CrawlResultat.Feila(rs.getString("feilmelding"), loeysing, sistOppdatert)
          }
          "ferdig" -> {
            val statusUrl = rs.getString("status_url")
            val antallNettsider = rs.getInt("ant_nettsider")
            CrawlResultat.Ferdig(antallNettsider, URI(statusUrl).toURL(), loeysing, sistOppdatert)
          }
          else -> throw RuntimeException("ukjent status lagret i databasen: $status")
        }

    return crawlResultat
  }

  @Transactional
  fun updateMaaling(maaling: Maaling) {
    if (maaling is Planlegging) {
      jdbcTemplate.update(
          "update MaalingV1 set navn = :navn, status = :status, max_lenker = :max_lenker, tal_lenker = :tal_lenker where id = :id",
          mapOf(
              "id" to maaling.id,
              "navn" to maaling.navn,
              "status" to "planlegging",
              "max_lenker" to maaling.crawlParameters.maxLenker,
              "tal_lenker" to maaling.crawlParameters.talLenker))

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
  fun saveMany(maalinger: Collection<Maaling>): Result<Collection<Maaling>> = runCatching {
    maalinger.forEach { save(it) }
    maalinger
  }

  @Transactional
  fun saveTestKoeyring(testKoeyring: TestKoeyring, maalingId: Int) {
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
              insert into testkoeyring(maaling_id, loeysing_id, status, status_url, sist_oppdatert, url_fullt_resultat, url_brot, url_agg_tr, url_agg_sk,url_agg_side, url_agg_side_tr, url_agg_loeysing)
              values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :url_fullt_resultat, :url_brot, :url_agg_tr, :url_agg_sk, :url_agg_side,:url_agg_side_tr,:url_agg_loeysing)
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
                    "url_agg_side" to testKoeyring.lenker?.urlAggregeringSide?.toString(),
                    "url_agg_side_tr" to testKoeyring.lenker?.urlAggregeringSideTR?.toString(),
                    "url_agg_loeysing" to testKoeyring.lenker?.urlAggregeringLoeysing?.toString()),
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

    if (crawlResultat is CrawlResultat.Ferdig) {
      val alreadyFinished =
          jdbcTemplate.queryForObject(
              "select count(*) from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id and status = :status_finished",
              mapOf(
                  "loeysingid" to crawlResultat.loeysing.id,
                  "maaling_id" to maalingId,
                  "status_finished" to "ferdig"),
              Int::class.java)

      if (alreadyFinished == 1) {
        logger.debug(
            "CrawlResultat.Ferdig hopper over for maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id}")
        return
      }
    }

    jdbcTemplate.update(
        "delete from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
        mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maalingId))
    when (crawlResultat) {
      is CrawlResultat.IkkjeStarta -> {
        jdbcTemplate.update(
            """
              insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) 
              values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)
            """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
            ))
      }
      is CrawlResultat.Starta -> {
        jdbcTemplate.update(
            """
              insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert, lenker_crawla) 
              values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert, :lenker_crawla)
            """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "lenker_crawla" to crawlResultat.framgang.prosessert))
      }
      is CrawlResultat.Feila -> {
        jdbcTemplate.update(
            """
              insert into crawlresultat (loeysingid, status, maaling_id, sist_oppdatert, feilmelding)
              values (:loeysingid, :status, :maaling_id, :sist_oppdatert, :feilmelding)
            """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "feilmelding" to crawlResultat.feilmelding))
      }
      is CrawlResultat.Ferdig -> {
        logger.debug(
            "CrawlResultat.Ferdig insert start. maalingId: $maalingId, loeysingId: ${crawlResultat.loeysing.id}")

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update(
            "insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)",
            MapSqlParameterSource(
                mapOf(
                    "loeysingid" to crawlResultat.loeysing.id,
                    "status" to status(crawlResultat),
                    "status_url" to crawlResultat.statusUrl.toString(),
                    "maaling_id" to maalingId,
                    "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert))),
            keyHolder,
            arrayOf("id"))

        val id =
            keyHolder.key?.toInt() ?: throw RuntimeException("Feil ved insert av CrawlResultat")

        logger.debug(
            "CrawlResultat.Ferdig insert ferdig. maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id} ny crid: $id")

        crawlResultat.nettsider.forEach { nettside ->
          jdbcTemplate.update(
              "insert into nettside (crawlresultat_id, url) values (:cr_id, :url)",
              mapOf("cr_id" to id, "url" to nettside.toString()))
        }

        logger.debug(
            "CrawlResultat.Ferdig insert nettsider ferdig. maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id} crid: $id antall nettsider: ${crawlResultat.nettsider.size}")
      }
    }
  }

  private fun status(crawresultat: CrawlResultat): String =
      when (crawresultat) {
        is CrawlResultat.IkkjeStarta -> "ikkje_starta"
        is CrawlResultat.Starta -> "starta"
        is CrawlResultat.Feila -> "feila"
        is CrawlResultat.Ferdig -> "ferdig"
      }
}
