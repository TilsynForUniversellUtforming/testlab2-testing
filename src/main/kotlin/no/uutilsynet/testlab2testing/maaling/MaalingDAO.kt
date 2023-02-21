package no.uutilsynet.testlab2testing.maaling

import java.net.URL
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingLoysingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingLoysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingParams
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.createMaalingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.loeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.loysingRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingLoeysingSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.maalingRowmapper
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingByIdSql
import no.uutilsynet.testlab2testing.maaling.MaalingDAO.MaalingParams.selectMaalingSql
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

  data class MaalingDTO(
      val id: Int,
      val navn: String,
      val status: String,
  )

  object MaalingParams {
    val maalingRowmapper = DataClassRowMapper.newInstance(MaalingDTO::class.java)
    val loysingRowmapper = DataClassRowMapper.newInstance(Loeysing::class.java)

    val createMaalingSql = "insert into Maalingv1 (navn, status) values (:navn, :status)"
    fun createMaalingParams(navn: String) =
        MapSqlParameterSource("navn", navn).addValue("status", "planlegging")

    val createMaalingLoysingSql =
        "insert into MaalingLoeysing (idMaaling, idLoeysing) values (:idMaaling, :idLoeysing)"
    fun createMaalingLoysingParams(idMaaling: Int, idLoeysing: Int) =
        MapSqlParameterSource("idMaaling", idMaaling).addValue("idLoeysing", idLoeysing)

    val selectMaalingSql = "select id, navn, status from Maalingv1"
    val selectMaalingByIdSql = "$selectMaalingSql where id = :id"

    val maalingLoeysingSql =
        """ 
      select l.id, l.namn, l.url 
      from MaalingLoeysing ml 
        join loeysing l on ml.idLoeysing = l.id
      where ml.idMaaling = :id
      """
            .trimIndent()

    val saveMaalingSql = "update MaalingV1 set navn = :navn, status = :status where id = :id"
    fun saveMaalingParams(maaling: Maaling) =
        MapSqlParameterSource("navn", maaling.navn)
            .addValue("status", Maaling.status(maaling))
            .addValue("id", maaling.id)

    val loeysingSql = "select id, namn, url from loeysing"
  }

  @Transactional
  fun createMaaling(navn: String, loyesingIds: List<Int>): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()
    jdbcTemplate.update(createMaalingSql, createMaalingParams(navn), keyHolder, arrayOf("id"))
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

  fun getMaalingList(): List<Maaling> =
      jdbcTemplate.query(selectMaalingSql, maalingRowmapper).map { it.toMaaling() }

  private fun MaalingDTO.toMaaling(): Maaling {

    return when (this.status) {
      "planlegging" -> {
        val loeysingList =
            jdbcTemplate.query(
                maalingLoeysingSql, MapSqlParameterSource("id", id), loysingRowmapper)
        Maaling.Planlegging(this.id, this.navn, loeysingList)
      }
      "crawling" -> {
        val crawlResultat =
            jdbcTemplate.query(
                buildString {
                  appendLine("select cr.status_url, l.id, l.namn, l.url")
                  appendLine("from crawlresultat cr ")
                  appendLine("join loeysing l on cr.loeysingid = l.id")
                  appendLine("where maaling_id = :maalingId")
                  appendLine("and status = 'ikke_ferdig'")
                },
                mapOf("maalingId" to this.id)) { rs, _ ->
                  CrawlResultat.IkkeFerdig(
                      URL(rs.getString("status_url")),
                      Loeysing(rs.getInt("id"), rs.getString("namn"), URL(rs.getString("url"))))
                }
        Maaling.Crawling(this.id, this.navn, crawlResultat)
      }
      else ->
          throw RuntimeException("Målingen med id = $id er lagret med en ugyldig status: $status")
    }
  }

  @Transactional
  fun save(maaling: Maaling): Result<Maaling> =
      runCatching {
            when (maaling) {
              is Maaling.Planlegging -> {
                jdbcTemplate.update(
                    "update MaalingV1 set navn = :navn, status = :status where id = :id",
                    mapOf(
                        "navn" to maaling.navn,
                        "status" to Maaling.status(maaling),
                        "id" to maaling.id))
              }
              is Maaling.Crawling -> {
                jdbcTemplate.update(
                    "update MaalingV1 set navn = :navn, status = :status where id = :id",
                    mapOf(
                        "navn" to maaling.navn,
                        "status" to Maaling.status(maaling),
                        "id" to maaling.id))
                for (crawlResultat in maaling.crawlResultat) {
                  saveCrawlResultat(crawlResultat, maaling)
                }
              }
            }
          }
          .map { maaling }

  @Transactional
  fun saveCrawlResultat(crawlResultat: CrawlResultat, maaling: Maaling) {
    when (crawlResultat) {
      is CrawlResultat.IkkeFerdig -> {
        jdbcTemplate.update(
            "insert into crawlresultat (loeysingid, status, status_url, maaling_id) values (:loeysingid, :status, :status_url, :maaling_id)",
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to "ikke_ferdig",
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maaling.id))
      }
      is CrawlResultat.Feilet -> {
        jdbcTemplate.update(
            buildString {
              appendLine("insert into crawlresultat (loeysingid, status, maaling_id)")
              appendLine("values (:loeysingid, 'feilet', :maaling_id)")
            },
            mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maaling.id))
      }
    }
  }

  fun getLoeysingarList(): List<Loeysing> = jdbcTemplate.query(loeysingSql, loysingRowmapper)
}
