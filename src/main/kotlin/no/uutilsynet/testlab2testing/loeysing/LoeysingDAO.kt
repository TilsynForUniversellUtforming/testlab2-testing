package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.createLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingParams
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.deleteLoeysingSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.findLoeysingByNameSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingByIdSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingByOrgnummerSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingIdListSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.getLoeysingListSql
import no.uutilsynet.testlab2testing.loeysing.LoeysingDAO.LoeysingParams.loeysingRowMapper
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

    val getLoeysingBaseSql = "select id, namn, url, orgnummer from loeysing"
    val getLoeysingListSql = "$getLoeysingBaseSql order by id"
    val getLoeysingIdListSql = "select id from loeysing order by id"
    val getLoeysingByIdSql = "$getLoeysingBaseSql where id = :id"
    val findLoeysingByNameSql = "$getLoeysingBaseSql where lower(namn) like lower(:name_search)"
    val getLoeysingByOrgnummerSql = "$getLoeysingBaseSql where orgnummer = :orgnummer"

    val updateLoeysingSql =
        "update loeysing set namn = :namn, url = :url, orgnummer = :orgnummer where id = :id"

    fun updateLoeysingParams(loeysing: Loeysing) =
        mapOf(
            "namn" to loeysing.namn,
            "url" to loeysing.url.toString(),
            "orgnummer" to loeysing.orgnummer,
            "id" to loeysing.id)

    val deleteLoeysingSql = "delete from loeysing where id = :id"

    fun deleteLoeysingParams(id: Int) = mapOf("id" to id)

    val loeysingRowMapper = DataClassRowMapper.newInstance(Loeysing::class.java)
  }

  fun getLoeysing(id: Int): Loeysing? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getLoeysingByIdSql, mapOf("id" to id), loeysingRowMapper))

  fun findByName(nameSearch: String): List<Loeysing> =
      jdbcTemplate.query(
          findLoeysingByNameSql, mapOf("name_search" to "%$nameSearch%"), loeysingRowMapper)

  fun findByOrgnumber(orgnummer: String): List<Loeysing> =
      jdbcTemplate.query(
          getLoeysingByOrgnummerSql, mapOf("orgnummer" to orgnummer), loeysingRowMapper)

  fun getLoeysingList(): List<Loeysing> = jdbcTemplate.query(getLoeysingListSql, loeysingRowMapper)

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

  fun findLoeysingByURLAndOrgnummer(url: URL, orgnummer: String): Loeysing? {
    val sammeOrgnummer =
        jdbcTemplate.query(
            """
                select id, namn, url, orgnummer
                from loeysing
                where orgnummer = :orgnummer
            """
                .trimIndent(),
            mapOf("orgnummer" to orgnummer),
            loeysingRowMapper)
    return sammeOrgnummer.find { loeysing -> sameURL(loeysing.url, url) }
  }

  fun findLoeysingListForMaaling(maaling: Int): List<Loeysing> {
    return jdbcTemplate.query(
        """
            select id, namn, url, orgnummer
            from maalingloeysing ml
            join loeysing l
            on ml.idloeysing = l.id
            where ml.idmaaling = :maaling
        """
            .trimIndent(),
        mapOf("maaling" to maaling),
        loeysingRowMapper)
  }
}
