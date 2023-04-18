package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.dto.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingListSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loysingRowmapper
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.updateLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.updateLoeysingSql
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LoeysingDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object LoeysingParams {
    val createLoeysingSql = "insert into loeysing (namn, url) values (:namn, :url)"
    fun createLoeysingParams(namn: String, url: URL) =
        MapSqlParameterSource("namn", namn).addValue("url", url.toString())

    val getLoeysingListSql = "select id, namn, url from loeysing order by id"
    val getLoeysingSql = "select id, namn, url from loeysing where id = :id order by id"

    val updateLoeysingSql = "update loeysing set namn = :namn, url = :url where id = :id"
    fun updateLoeysingParams(loeysing: Loeysing) =
        MapSqlParameterSource("namn", loeysing.namn)
            .addValue("url", loeysing.url.toString())
            .addValue("id", loeysing.id)

    val deleteLoeysingSql = "delete from loeysing where id = :id"
    fun deleteLoeysingParams(id: Int) = MapSqlParameterSource("id", id)

    val loysingRowmapper = DataClassRowMapper.newInstance(Loeysing::class.java)
  }

  fun getLoeysing(id: Int): Loeysing? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getLoeysingSql, MapSqlParameterSource("id", id), loysingRowmapper))

  fun getLoeysingList(): List<Loeysing> = jdbcTemplate.query(getLoeysingListSql, loysingRowmapper)

  @Transactional
  fun createLoeysing(namn: String, url: URL): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()
    jdbcTemplate.update(
        createLoeysingSql, createLoeysingParams(namn, url), keyHolder, arrayOf("id"))
    return keyHolder.key!!.toInt()
  }

  @Transactional
  fun deleteLoeysing(id: Int) = jdbcTemplate.update(deleteLoeysingSql, deleteLoeysingParams(id))

  @Transactional
  fun updateLoeysing(loeysing: Loeysing) =
      jdbcTemplate.update(updateLoeysingSql, updateLoeysingParams(loeysing))
}
