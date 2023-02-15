package no.uutilsynet.testlab2testing.testreglar

import no.uutilsynet.testlab2testing.dto.Regelsett
import no.uutilsynet.testlab2testing.dto.TestregelDTO
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteRegelsettParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteRegelsettSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteRegelsettTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteRegelsettTestregelSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsett
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettTestregel
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertTestregelSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.listRegelsettSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.listTestreglarSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.regelsettListDAORowmapper
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.testregelRowmapper
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.updateRegelsettNameParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.updateRegelsettNameSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.updateTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.updateTestregelSql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestregelDAO(@Autowired val jdbcTemplate: NamedParameterJdbcTemplate) : TestregelApi {

  data class RegelsettListDTO(
      val regelsettId: Int,
      val regelsettNamn: String,
      val testregelId: Int,
      val testregelKravId: Int?,
      val testregelReferanseAct: String?,
      val testregelKravTilSamsvar: String,
      val testregelType: String,
      val testregelStatus: String,
  )

  object TestregelParams {
    val testregelRowmapper = DataClassRowMapper.newInstance(TestregelDTO::class.java)
    val regelsettListDAORowmapper = DataClassRowMapper.newInstance(RegelsettListDTO::class.java)

    /* listTestreglar */
    val listTestreglarSql =
        """
            select
            tr.id,
            tr.kravId,
            tr.referanseAct,
            tr.kravTilSamsvar,
            tr.type,
            tr.status
            from testregel tr
            order by tr.id
        """
            .trimIndent()

    /* listRegelsett */
    val listRegelsettSql =
        """
            select 
              rs.id as regelsettId,
              rs.namn as regelsettNamn,
              tr.id as testregelId,
              tr.kravId as testregelKravId,
              tr.referanseAct as testregelReferanseAct,
              tr.kravTilSamsvar as testregelKravTilSamsvar,
              tr.type as testregelType,
              tr.status as testregelStatus
            from regelsett rs
            join regelsetttestregel trt on rs.id = trt.idregelsett
            join testregel tr on trt.idtestregel = tr.id
            order by rs.id, tr.id
        """
            .trimIndent()

    /* createTestregel  */
    val insertTestregelSql =
        "insert into testregel (kravId, referanseAct, kravTilSamsvar, status, type) values (:kravId, :referanseAct, :kravTilSamsvar, :status, :type)"
    fun insertTestregelParameters(testregelRequest: TestregelRequest) =
        MapSqlParameterSource("kravId", testregelRequest.kravId)
            .addValue("referanseAct", testregelRequest.referanseAct)
            .addValue("kravTilSamsvar", testregelRequest.kravTilSamsvar)
            .addValue("status", testregelRequest.status)
            .addValue("type", testregelRequest.type)

    /* createRegelsett */
    val insertRegelsett = "insert into regelsett (namn) values (:namn)"
    fun insertRegelsettParameters(namn: String) = MapSqlParameterSource("namn", namn)

    val insertRegelsettTestregel =
        "insert into RegelsettTestregel (idRegelsett, idTestregel) values (:idRegelsett, :idTestregel)"
    fun insertRegelsettTestregelParameters(idRegelsett: Int, idTestregel: Int) =
        MapSqlParameterSource("idRegelsett", idRegelsett).addValue("idTestregel", idTestregel)

    /* updateTestregel */
    val updateTestregelSql =
        """
        update testregel set 
            kravId = :kravId,
            referanseAct = :referanseAct,
            kravTilSamsvar = :kravTilSamsvar,
            status = :status,
            type = :type
        where id = :id
    """
            .trimIndent()
    fun updateTestregelParameters(testregel: TestregelDTO) =
        MapSqlParameterSource("kravId", testregel.kravId)
            .addValue("referanseAct", testregel.referanseAct)
            .addValue("kravTilSamsvar", testregel.kravTilSamsvar)
            .addValue("status", testregel.status)
            .addValue("type", testregel.type)
            .addValue("id", testregel.id)

    /* updateRegelsett */
    val updateRegelsettNameSql = "update regelsett set namn = :namn where id = :id"
    fun updateRegelsettNameParameters(namn: String, id: Int) =
        MapSqlParameterSource("namn", namn).addValue("id", id)
    val deleteRegelsettTestregelSql =
        "delete from regelsetttestregel where idregelsett = :idregelsett"
    fun deleteRegelsettTestregelParameters(idregelsett: Int) =
        MapSqlParameterSource("idregelsett", idregelsett)

    /* deleteTestregel */
    val deleteTestregelSql = "delete from testregel where id = :id"
    fun deleteTestregelParameters(id: Int) = MapSqlParameterSource("id", id)

    /* deleteRegelsett */
    val deleteRegelsettSql = "delete from regelsett where id = :id"
    fun deleteRegelsettParameters(id: Int) = MapSqlParameterSource("id", id)
  }

  override fun listTestreglar(): List<TestregelDTO> =
      jdbcTemplate.query(listTestreglarSql, testregelRowmapper)

  override fun listRegelsett(): List<Regelsett> =
      jdbcTemplate
          .query(listRegelsettSql, regelsettListDAORowmapper)
          .groupBy { it.regelsettId to it.regelsettNamn }
          .map {
            Regelsett(
                it.key.first,
                it.key.second,
                it.value
                    .map { tr ->
                      TestregelDTO(
                          tr.testregelId,
                          tr.testregelKravId,
                          tr.testregelReferanseAct,
                          tr.testregelKravTilSamsvar,
                          tr.testregelType,
                          tr.testregelStatus
                      )
                    }
                    .toList())
          }

  @Transactional
  override fun createTestregel(testregelRequest: TestregelRequest): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()

    jdbcTemplate.update(
        insertTestregelSql, insertTestregelParameters(testregelRequest), keyHolder, arrayOf("id"))
    return keyHolder.key!!.toInt()
  }

  @Transactional
  override fun createRegelsett(regelsettRequest: RegelsettRequest): Int {
    val keyHolder: KeyHolder = GeneratedKeyHolder()

    jdbcTemplate.update(
        insertRegelsett, insertRegelsettParameters(regelsettRequest.namn), keyHolder, arrayOf("id"))
    val regelsettId = keyHolder.key!!.toInt()

    for (id in regelsettRequest.ids) {
      jdbcTemplate.update(
          insertRegelsettTestregel, insertRegelsettTestregelParameters(regelsettId, id))
    }

    return regelsettId
  }

  @Transactional
  override fun updateTestregel(testregel: TestregelDTO): TestregelDTO {
    jdbcTemplate.update(updateTestregelSql, updateTestregelParameters(testregel))
    return testregel
  }

  @Transactional
  override fun updateRegelsett(regelsett: Regelsett): Regelsett {
    val (id, namn) = regelsett
    val testRegelIdList = regelsett.testregelList.map { it.id }
    jdbcTemplate.update(updateRegelsettNameSql, updateRegelsettNameParameters(namn, id))
    jdbcTemplate.update(deleteRegelsettTestregelSql, deleteRegelsettTestregelParameters(id))
    for (idTestRegel in testRegelIdList) {
      jdbcTemplate.update(
          insertRegelsettTestregel, insertRegelsettTestregelParameters(id, idTestRegel))
    }

    return regelsett
  }

  @Transactional
  override fun deleteTestregel(id: Int) {
    jdbcTemplate.update(deleteTestregelSql, deleteTestregelParameters(id))
  }

  @Transactional
  override fun deleteRegelsett(id: Int) {
    jdbcTemplate.update(deleteRegelsettSql, deleteRegelsettParameters(id))
  }
}
