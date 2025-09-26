package no.uutilsynet.testlab2testing.testing.automatisk

import java.net.URI
import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.forenkletkontroll.Framgang
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestkoeyringDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val brukarService: BrukarService
) {

  fun getTestKoeyringarForMaaling(
      maalingId: Int,
      crawlResultatMap: Map<Int, CrawlResultat.Ferdig>
  ): List<TestKoeyring> {
    return jdbcTemplate.query<TestKoeyring>(
        """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side,url_agg_side_tr,url_agg_loeysing, brukar_id
              from "testlab2_testing"."testkoeyring" t
              where maaling_id = :maaling_id
            """
            .trimIndent(),
        mapOf("maaling_id" to maalingId),
        fun(rs: ResultSet, _: Int): TestKoeyring {
          val status = rs.getString("status")
          val loeysingId = rs.getInt("loeysing_id")
          val brukar = getBrukarFromResultSet(rs)
          val crawlResultatForLoeysing =
              crawlResultatMap[loeysingId]
                  ?: throw RuntimeException(
                      "Finner ikkje crawlresultat for loeysing med id = $loeysingId")

          val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
          return when (status) {
            "ikkje_starta" -> {
              ikkjeStarta(
                  crawlResultatForLoeysing.loeysing,
                  sistOppdatert,
                  crawlResultatForLoeysing.antallNettsider,
                  rs,
                  brukar)
            }
            "starta" -> {
              starta(
                  crawlResultatForLoeysing.loeysing,
                  sistOppdatert,
                  crawlResultatForLoeysing.antallNettsider,
                  rs,
                  brukar)
            }
            "feila" -> feila(crawlResultatForLoeysing.loeysing, sistOppdatert, rs, brukar)
            "ferdig" -> {
              ferdig(
                  rs,
                  crawlResultatForLoeysing.loeysing,
                  sistOppdatert,
                  crawlResultatForLoeysing.antallNettsider,
                  brukar)
            }
            else -> throw RuntimeException("ukjent status $status")
          }
        })
  }

  @Transactional
  fun saveTestKoeyring(testKoeyring: TestKoeyring, maalingId: Int) {
    deleteExistingTestkoeyring(maalingId, testKoeyring.loeysing.id)
    when (testKoeyring) {
      is TestKoeyring.Starta -> saveTestKoeyringStarta(maalingId, testKoeyring)
      is TestKoeyring.Ferdig -> saveTestKoeyringFerdig(maalingId, testKoeyring)
      else -> saveNyTestKoeyring(maalingId, testKoeyring)
    }
  }

  private fun saveTestKoeyringStarta(maalingId: Int, testKoeyring: TestKoeyring.Starta) {
    jdbcTemplate.update(
        """insert into "testlab2_testing"."testkoeyring" (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, lenker_testa, brukar_id) 
                    values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding, :lenker_testa,:brukar_id)
                """
            .trimMargin(),
        mapOf(
            "maaling_id" to maalingId,
            "loeysing_id" to testKoeyring.loeysing.id,
            "status" to status(testKoeyring),
            "status_url" to statusURL(testKoeyring),
            "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
            "feilmelding" to feilmelding(testKoeyring),
            "lenker_testa" to testKoeyring.framgang.prosessert,
            "brukar_id" to getBrukar(testKoeyring.brukar)))
  }

  private fun saveTestKoeyringFerdig(maalingId: Int, testKoeyring: TestKoeyring.Ferdig) {
    jdbcTemplate.update(
        """
                  insert into "testlab2_testing"."testkoeyring"(maaling_id, loeysing_id, status, status_url, sist_oppdatert, url_fullt_resultat, url_brot, url_agg_tr, url_agg_sk,url_agg_side, url_agg_side_tr, url_agg_loeysing,brukar_id)
                  values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :url_fullt_resultat, :url_brot, :url_agg_tr, :url_agg_sk, :url_agg_side,:url_agg_side_tr,:url_agg_loeysing,:brukar_id)
                """
            .trimIndent(),
        mapOf(
            "maaling_id" to maalingId,
            "loeysing_id" to testKoeyring.loeysing.id,
            "status" to status(testKoeyring),
            "status_url" to statusURL(testKoeyring),
            "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
            "url_fullt_resultat" to testKoeyring.lenker?.urlFulltResultat?.toString(),
            "url_brot" to testKoeyring.lenker?.urlBrot?.toString(),
            "url_agg_tr" to testKoeyring.lenker?.urlAggregeringTR?.toString(),
            "url_agg_sk" to testKoeyring.lenker?.urlAggregeringSK?.toString(),
            "url_agg_side" to testKoeyring.lenker?.urlAggregeringSide?.toString(),
            "url_agg_side_tr" to testKoeyring.lenker?.urlAggregeringSideTR?.toString(),
            "url_agg_loeysing" to testKoeyring.lenker?.urlAggregeringLoeysing?.toString(),
            "brukar_id" to getBrukar(testKoeyring.brukar)))
  }

  fun saveNyTestKoeyring(maalingId: Int, testKoeyring: TestKoeyring) {
    jdbcTemplate.queryForObject(
        """insert into "testlab2_testing"."testkoeyring" (maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, brukar_id) 
                    values (:maaling_id, :loeysing_id, :status, :status_url, :sist_oppdatert, :feilmelding, :brukar_id)
                    returning id
                """
            .trimMargin(),
        mapOf(
            "maaling_id" to maalingId,
            "loeysing_id" to testKoeyring.loeysing.id,
            "status" to status(testKoeyring),
            "status_url" to statusURL(testKoeyring),
            "sist_oppdatert" to Timestamp.from(testKoeyring.sistOppdatert),
            "feilmelding" to feilmelding(testKoeyring),
            "brukar_id" to getBrukar(testKoeyring.brukar)),
        Int::class.java)
  }

  fun deleteExistingTestkoeyring(maalingId: Int, loeysingId: Int) {
    jdbcTemplate.update(
        """delete from "testlab2_testing"."testkoeyring" where maaling_id = :maaling_id and loeysing_id = :loeysing_id""",
        mapOf("maaling_id" to maalingId, "loeysing_id" to loeysingId))
  }

  private fun feilmelding(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.Feila -> testKoeyring.feilmelding
        else -> null
      }

  private fun status(testKoeyring: TestKoeyring): String =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> "ikkje_starta"
        is TestKoeyring.Starta -> "starta"
        is TestKoeyring.Feila -> "feila"
        is TestKoeyring.Ferdig -> "ferdig"
      }

  private fun getBrukar(brukar: Brukar?) = brukar?.let { brukarService.getUserId(it) }

  fun getTestarTestkoeyringar(maalingId: Int): List<Brukar> {
    val query =
        """
            select b.id, brukarnamn, namn from testkoeyring tk join brukar b on tk.brukar_id = b.id where maaling_id = :maalingId
            
        """
            .trimIndent()
    return jdbcTemplate.query(
        query,
        mapOf("maalingId" to maalingId),
        fun(rs: ResultSet, _: Int): Brukar {
          return Brukar(rs.getString("brukarnamn"), rs.getString("namn"))
        })
  }

  private fun getBrukarFromResultSet(rs: ResultSet) =
      brukarService.getBrukarById(rs.getInt("brukar_id"))

  private fun statusURL(testKoeyring: TestKoeyring): String? =
      when (testKoeyring) {
        is TestKoeyring.IkkjeStarta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Starta -> testKoeyring.statusURL.toString()
        is TestKoeyring.Ferdig -> testKoeyring.statusURL.toString()
        else -> null
      }

  private fun feila(
      loeysing: Loeysing,
      sistOppdatert: Instant,
      rs: ResultSet,
      brukar: Brukar?,
  ) = TestKoeyring.Feila(loeysing, sistOppdatert, rs.getString("feilmelding"), brukar)

  private fun starta(
      loeysing: Loeysing,
      sistOppdatert: Instant,
      antallNettsider: Int,
      rs: ResultSet,
      brukar: Brukar?
  ) =
      TestKoeyring.Starta(
          loeysing,
          sistOppdatert,
          statusUrlFromResultSet(rs),
          Framgang(rs.getInt("lenker_testa"), antallNettsider),
          brukar,
          antallNettsider)

  private fun ikkjeStarta(
      loeysing: Loeysing,
      sistOppdatert: Instant,
      antallNettsider: Int,
      rs: ResultSet,
      brukar: Brukar?
  ) =
      TestKoeyring.IkkjeStarta(
          loeysing, sistOppdatert, statusUrlFromResultSet(rs), brukar, antallNettsider)

  private fun ferdig(
      rs: ResultSet,
      loeysing: Loeysing,
      sistOppdatert: Instant,
      antallNettsider: Int,
      brukar: Brukar?
  ): TestKoeyring.Ferdig {
    val urlFulltResultat = rs.getString("url_fullt_resultat")
    val urlBrot = rs.getString("url_brot")
    val urlAggTR = rs.getString("url_agg_tr")
    val urlAggSK = rs.getString("url_agg_sk")
    val urlAggSide = rs.getString("url_agg_side")
    val urlAggSideTR = rs.getString("url_agg_side_tr")
    val urlAggLoeysing = rs.getString("url_agg_loeysing")

    val lenker =
        autoTesterLenker(
            urlFulltResultat, urlBrot, urlAggTR, urlAggSK, urlAggSide, urlAggSideTR, urlAggLoeysing)
    return TestKoeyring.Ferdig(
        loeysing, sistOppdatert, statusUrlFromResultSet(rs), lenker, brukar, antallNettsider)
  }

  private fun statusUrlFromResultSet(rs: ResultSet): URL = URI(rs.getString("status_url")).toURL()

  private fun autoTesterLenker(
      urlFulltResultat: String?,
      urlBrot: String?,
      urlAggTR: String?,
      urlAggSK: String?,
      urlAggSide: String?,
      urlAggSideTR: String?,
      urlAggLoeysing: String?
  ): AutoTesterClient.AutoTesterLenker? {

    val lenker =
        if (urlFulltResultat != null)
            AutoTesterClient.AutoTesterLenker(
                verifiedAutotestlenker(urlFulltResultat),
                verifiedAutotestlenker(urlBrot),
                verifiedAutotestlenker(urlAggTR),
                verifiedAutotestlenker(urlAggSK),
                verifiedAutotestlenker(urlAggSide),
                verifiedAutotestlenker(urlAggSideTR),
                verifiedAutotestlenker(urlAggLoeysing))
        else null
    return lenker
  }

  private fun verifiedAutotestlenker(autotesterLenke: String?): URL {
    requireNotNull(autotesterLenke) { "autotestlenke er null" }
    return URI(autotesterLenke).toURL()
  }
}
