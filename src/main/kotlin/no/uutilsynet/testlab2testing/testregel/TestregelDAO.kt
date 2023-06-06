package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.createTestregelParams
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.createTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelListSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelListByIdSql
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
        "insert into testregel (krav, referanseAct, kravtilsamsvar) values (:krav, :referanseAct, :kravtilsamsvar) returning id"
    fun createTestregelParams(krav: String, referanseAct: String, kravtilsamsvar: String) =
        mapOf("krav" to krav, "referanseAct" to referanseAct, "kravtilsamsvar" to kravtilsamsvar)

    val getTestregelListSql =
        "select id, krav, referanseAct, kravtilsamsvar from testregel order by id"

    val getTestregelSql =
        "select id, krav, referanseAct, kravtilsamsvar from testregel where id = :id order by id"

    val updateTestregelSql =
        " update testregel set krav = :krav, referanseAct = :referanseAct, kravtilsamsvar = :kravtilsamsvar where id = :id"
    fun updateTestregelParams(testregel: Testregel) =
        mapOf(
            "id" to testregel.id,
            "krav" to testregel.krav,
            "referanseAct" to testregel.referanseAct,
            "kravtilsamsvar" to testregel.kravTilSamsvar)

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = "delete from testregel where id = :id"

    val maalingTestregelListByIdSql =
        "select maaling_id from maaling_testregel where testregel_id = :testregel_id"
  }

  fun getTestregel(id: Int): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getTestregelSql, mapOf("id" to id), testregelRowMapper))

  fun getTestregelList(): List<Testregel> =
      jdbcTemplate.query(getTestregelListSql, testregelRowMapper)

  @Transactional
  fun createTestregel(
      krav: String,
      referanseAct: String,
      kravtilsamsvar: String,
  ): Int =
      jdbcTemplate.queryForObject(
          createTestregelSql,
          createTestregelParams(krav, referanseAct, kravtilsamsvar),
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
}
