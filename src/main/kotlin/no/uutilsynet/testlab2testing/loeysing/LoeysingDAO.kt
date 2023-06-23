package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingIdListSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingListSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loysingRowmapper
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.updateLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.updateLoeysingSql
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LoeysingDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object LoeysingParams {
    val createLoeysingSql =
        "insert into loeysing (namn, url, orgnummer) values (:namn, :url, :orgnummer) returning id"
    fun createLoeysingParams(namn: String, url: URL, orgnummer: String?) =
        mapOf("namn" to namn, "url" to url.toString(), "orgnummer" to orgnummer)

    val getLoeysingListSql = "select id, namn, url, orgnummer from loeysing order by id"
    val getLoeysingIdListSql = "select id from loeysing order by id"
    val getLoeysingSql = "select id, namn, url, orgnummer from loeysing where id = :id order by id"

    val updateLoeysingSql = "update loeysing set namn = :namn, url = :url where id = :id"
    fun updateLoeysingParams(loeysing: Loeysing) =
        mapOf("namn" to loeysing.namn, "url" to loeysing.url.toString(), "id" to loeysing.id)

    val deleteLoeysingSql = "delete from loeysing where id = :id"
    fun deleteLoeysingParams(id: Int) = mapOf("id" to id)

    val loysingRowmapper = DataClassRowMapper.newInstance(Loeysing::class.java)
  }

  fun getLoeysing(id: Int): Loeysing? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getLoeysingSql, mapOf("id" to id), loysingRowmapper))

  fun getLoeysingList(): List<Loeysing> = jdbcTemplate.query(getLoeysingListSql, loysingRowmapper)

  @Transactional
  fun createLoeysing(namn: String, url: URL, orgnummer: String?): Int =
      jdbcTemplate.queryForObject(
          createLoeysingSql, createLoeysingParams(namn, url, orgnummer), Int::class.java)!!

  @Transactional
  fun deleteLoeysing(id: Int) = jdbcTemplate.update(deleteLoeysingSql, deleteLoeysingParams(id))

  @Transactional
  fun updateLoeysing(loeysing: Loeysing) =
      jdbcTemplate.update(updateLoeysingSql, updateLoeysingParams(loeysing))

  fun getLoeysingIdList(): List<Int> =
      jdbcTemplate.queryForList(getLoeysingIdListSql, emptyMap<String, String>(), Int::class.java)

  fun getMaalingLoeysingListById(idloeysing: Int): List<Int> =
      jdbcTemplate.queryForList(
          "select idmaaling from maalingLoeysing where idloeysing in (:idloeysing)",
          mapOf("idloeysing" to idloeysing),
          Int::class.java)
}
