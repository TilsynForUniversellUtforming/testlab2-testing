package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.aggregering.AggregeringService
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.Crawling
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.Kvalitetssikring
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.Planlegging
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.Testing
import no.uutilsynet.testlab2testing.forenkletkontroll.Maaling.TestingFerdig
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.crawlParametersRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.deleteMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByDateSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByIdSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.selectMaalingByStatus
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.updateMaalingParams
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO.MaalingParams.updateMaalingSql
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.crawling
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.kvalitetssikring
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.planlegging
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.testing
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingStatus.testing_ferdig
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.loeysing.Utval
import no.uutilsynet.testlab2testing.loeysing.UtvalId
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import org.slf4j.LoggerFactory
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MaalingDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val aggregeringService: AggregeringService,
    val sideutvalDAO: SideutvalDAO
) {

  private val logger = LoggerFactory.getLogger(MaalingDAO::class.java)

  data class MaalingDTO(
      val id: Int,
      val navn: String,
      val datoStart: Instant,
      val status: MaalingStatus,
      val maxLenker: Int,
      val talLenker: Int
  )

  object MaalingParams {
    val maalingRowmapper = DataClassRowMapper.newInstance(MaalingDTO::class.java)
    val crawlParametersRowmapper = DataClassRowMapper.newInstance(CrawlParameters::class.java)

    val createMaalingSql =
        """
      insert into Maalingv1 (navn, status, dato_start, max_lenker, tal_lenker, utval_id) 
      values (:navn, :status, :dato_start, :max_lenker, :tal_lenker, :utvalId)
      returning id
    """
            .trimIndent()

    fun createMaalingParams(
        navn: String,
        datoStart: Instant,
        crawlParameters: CrawlParameters,
        utvalId: UtvalId? = null
    ) =
        mapOf(
            "navn" to navn,
            "dato_start" to Timestamp.from(datoStart),
            "status" to "planlegging",
            "max_lenker" to crawlParameters.maxLenker,
            "tal_lenker" to crawlParameters.talLenker,
            "utvalId" to utvalId)

    val selectMaalingSql =
        "select id, navn, dato_start, status, max_lenker, tal_lenker from Maalingv1"

    val selectMaalingByDateSql = "$selectMaalingSql order by dato_start desc"

    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val selectMaalingByStatus = "$selectMaalingSql where status in (:statusList)"

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
      datoStart: Instant,
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
      datoStart: Instant,
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
    val loeysingIdList: List<Int> =
        jdbcTemplate.queryForList(
            "select idloeysing from maalingloeysing where idmaaling = :id",
            mapOf("id" to id),
            Int::class.java)
    val loeysingList = loeysingsRegisterClient.getMany(loeysingIdList, datoStart).getOrThrow()
    return when (status) {
      planlegging -> {
        val testregelList =
            jdbcTemplate.query(maalingTestregelSql, mapOf("id" to id), testregelRowMapper).map {
              it.toTestregelBase()
            }
        Planlegging(
            id, navn, datoStart, loeysingList, testregelList, CrawlParameters(maxLenker, talLenker))
      }
      crawling,
      kvalitetssikring -> {
        val crawlResultat = sideutvalDAO.getCrawlResultatForMaaling(id, loeysingList)
        if (status == crawling) {
          Crawling(this.id, this.navn, this.datoStart, crawlResultat)
        } else {
          Kvalitetssikring(id, navn, datoStart, crawlResultat)
        }
      }
      testing,
      testing_ferdig -> {
        val testKoeyringar = getTestKoeyringarForMaaling(id, loeysingList)
        if (status == testing) {
          Testing(id, navn, datoStart, testKoeyringar)
        } else {
          TestingFerdig(id, navn, datoStart, testKoeyringar)
        }
      }
    }
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

  private fun getTestKoeyringarForMaaling(
      maalingId: Int,
      loeysingList: List<Loeysing>
  ): List<TestKoeyring> {
    val crawlResultat = sideutvalDAO.getCrawlResultatForMaaling(maalingId, loeysingList)
    return jdbcTemplate.query<TestKoeyring>(
        """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side,url_agg_side_tr,url_agg_loeysing
              from testkoeyring t
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
              val urlFulltResultat = rs.getString("url_fullt_resultat")
              val urlBrot = rs.getString("url_brot")
              val urlAggTR = rs.getString("url_agg_tr")
              val urlAggSK = rs.getString("url_agg_sk")
              val urlAggSide = rs.getString("url_agg_side")
              val urlAggSideTR = rs.getString("url_agg_side_tr")
              val urlAggLoeysing = rs.getString("url_agg_loeysing")

              val lenker =
                  if (urlFulltResultat != null)
                      AutoTesterClient.AutoTesterLenker(
                          URI(urlFulltResultat).toURL(),
                          URI(urlBrot).toURL(),
                          URI(urlAggTR).toURL(),
                          URI(urlAggSK).toURL(),
                          URI(urlAggSide).toURL(),
                          URI(urlAggSideTR).toURL(),
                          URI(urlAggLoeysing).toURL())
                  else null
              TestKoeyring.Ferdig(
                  crawlResultatForLoeysing,
                  sistOppdatert,
                  URI(rs.getString("status_url")).toURL(),
                  lenker)
            }
            else -> throw RuntimeException("ukjent status $status")
          }
        })
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
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Kvalitetssikring -> {
        maaling.crawlResultat.forEach { sideutvalDAO.saveCrawlResultat(it, maaling.id) }
      }
      is Testing -> {
        maaling.testKoeyringar.forEach { saveTestKoeyring(it, maaling.id) }
      }
      is TestingFerdig -> {
        maaling.testKoeyringar.forEach {
          saveTestKoeyring(it, maaling.id)
          aggregeringService.saveAggregering(it as TestKoeyring.Ferdig)
        }
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
    when (testKoeyring) {
      is TestKoeyring.Starta -> {
        saveTestKoeyringStarta(maalingId, testKoeyring)
      }
      is TestKoeyring.Ferdig -> {
        saveTestKoeyringFerdig(maalingId, testKoeyring)
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
  }

  private fun saveTestKoeyringFerdig(maalingId: Int, testKoeyring: TestKoeyring.Ferdig) {
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

  private fun saveTestKoeyringStarta(maalingId: Int, testKoeyring: TestKoeyring.Starta) {
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
}
