package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelListSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelListByIdSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestregelDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object TestregelParams {

    val getTestregelListSql =
        "select id, name, krav, testregel_schema, type  from testregel order by id"

    val getTestregelSql =
        "select id, name, krav, testregel_schema, type from testregel where id = :id order by id"

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = "delete from testregel where id = :id"

    val maalingTestregelListByIdSql =
        "select maaling_id from maaling_testregel where testregel_id = :testregel_id"

    val maalingTestregelSql =
        """
      select 
      tr.id,
      tr.name,
      tr.krav,
      tr.testregel_schema,
      tr.type
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
  fun createTestregel(testregelInit: TestregelInit): Int =
      jdbcTemplate.queryForObject(
          "insert into testregel (name, testregel_schema, type, krav) values (:name, :testregel_schema, :type, :krav) returning id",
          mapOf(
              "name" to testregelInit.name,
              "testregel_schema" to testregelInit.testregelSchema,
              "type" to testregelInit.type.value,
              "krav" to testregelInit.krav),
          Int::class.java)!!

  @Transactional
  fun updateTestregel(testregel: Testregel) =
      jdbcTemplate.update(
          " update testregel set name = :name, krav = :krav, testregel_schema = :testregel_schema where id = :id",
          mapOf(
              "id" to testregel.id,
              "name" to testregel.name,
              "krav" to testregel.krav,
              "testregel_schema" to testregel.testregelSchema,
          ))

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
