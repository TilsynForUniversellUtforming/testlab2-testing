package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.createTestregelParams
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.createTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelListSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelListByIdSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.updateTestregelParams
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.updateTestregelSql
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestregelDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object TestregelParams {
    val createTestregelSql =
        "insert into testregel (krav, testregelNoekkel, kravtilsamsvar) values (:krav, :testregelNoekkel, :kravtilsamsvar) returning id"

    fun createTestregelParams(krav: String, testregelNoekkel: String, kravtilsamsvar: String) =
        mapOf(
            "krav" to krav,
            "testregelNoekkel" to testregelNoekkel,
            "kravtilsamsvar" to kravtilsamsvar)

    val getTestregelListSql =
        "select id, krav, testregelNoekkel, kravtilsamsvar from testregel order by id"

    val getTestregelSql =
        "select id, krav, testregelNoekkel, kravtilsamsvar from testregel where id = :id order by id"

    val updateTestregelSql =
        " update testregel set krav = :krav, testregelNoekkel = :testregelNoekkel, kravtilsamsvar = :kravtilsamsvar where id = :id"

    fun updateTestregelParams(testregel: Testregel) =
        mapOf(
            "id" to testregel.id,
            "krav" to testregel.krav,
            "testregelNoekkel" to testregel.testregelNoekkel,
            "kravtilsamsvar" to testregel.kravTilSamsvar)

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = "delete from testregel where id = :id"

    val maalingTestregelListByIdSql =
        "select maaling_id from maaling_testregel where testregel_id = :testregel_id"

    val maalingTestregelSql =
        """
      select 
      tr.id,
      tr.krav,
      tr.testregelNoekkel,
      tr.kravtilsamsvar
      from MaalingV1 m
        join Maaling_Testregel mt on m.id = mt.maaling_id
        join Testregel tr on mt.testregel_id = tr.id
      where m.id = :id
    """
            .trimIndent()
  }

  fun getTestregel(id: Int): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getTestregelSql, mapOf("id" to id), testregelRowMapper))

  fun getTestregelList(): List<Testregel> =
      jdbcTemplate.query(getTestregelListSql, testregelRowMapper)

  @Transactional
  fun createTestregel(
      krav: String,
      testregelNoekkel: String,
      kravtilsamsvar: String,
  ): Int =
      jdbcTemplate.queryForObject(
          createTestregelSql,
          createTestregelParams(krav, testregelNoekkel, kravtilsamsvar),
          Int::class.java)!!

  @Transactional
  fun updateTestregel(testregel: Testregel) =
      jdbcTemplate.update(updateTestregelSql, updateTestregelParams(testregel))

  @Transactional
  fun deleteTestregel(testregelId: Int) =
      jdbcTemplate.update(deleteTestregelSql, mapOf("id" to testregelId))

  fun getMaalingTestregelListById(testregelId: Int): List<Int> =
      jdbcTemplate.queryForList(
          maalingTestregelListByIdSql, mapOf("testregel_id" to testregelId), Int::class.java)

  fun getTestreglarForMaaling(maalingId: Int): Result<List<Testregel>> = runCatching {
    jdbcTemplate.query(maalingTestregelSql, mapOf("id" to maalingId), testregelRowMapper)
  }
}
