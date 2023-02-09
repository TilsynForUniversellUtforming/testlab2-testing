package no.uutilsynet.testlab2testing.testreglar

import no.uutilsynet.testlab2testing.dto.Regelsett
import no.uutilsynet.testlab2testing.dto.Testregel
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteRegelsettSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsett
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettTestregel
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.insertRegelsettTestregelParameters
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.listRegelsettSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.listTestreglarSql
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.regelsettListDAORowmapper
import no.uutilsynet.testlab2testing.testreglar.TestregelDAO.TestregelParams.testregelRowmapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class TestregelDAO(@Autowired val jdbcTemplate: NamedParameterJdbcTemplate): TestregelApi {

  data class RegelsettListDTO(
    val regelsettId: Int,
    val regelsettNamn: String,
    val testregelId: Int,
    val testregelKravId: Int?,
    val testregelReferanseAct: String,
    val testregelKravTilSamsvar: String,
    val testregelType: String,
    val testregelStatus: String,
    val kravTittel: String?
  )

  object TestregelParams {
    val testregelRowmapper = DataClassRowMapper.newInstance(Testregel::class.java)
    val regelsettListDAORowmapper = DataClassRowMapper.newInstance(RegelsettListDTO::class.java)

    val listTestreglarSql =
      """
            select
            tr.id,
            tr.kravId,
            tr.referanseAct,
            tr.kravTilSamsvar,
            tr.type,
            tr.status,
            k.tittel as kravTittel
            from testregel tr
                left join testlab2_krav.krav k on tr.kravid = k.id
        """
        .trimIndent()

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
              tr.status as testregelStatus,
              k.tittel as kravTittel
            from regelsett rs
            join regelsetttestregel trt on rs.id = trt.idregelsett
            join testregel tr on trt.idtestregel = tr.id
                left join testlab2_krav.krav k on tr.kravid = k.id
        """
        .trimIndent()

    val insertTestregel = ""

    val insertRegelsett = "insert into regelsett (namn) values (:namn)"
    fun insertRegelsettParameters(namn: String) =
      MapSqlParameterSource("namn", namn)

    val insertRegelsettTestregel = "insert into RegelsettTestregel (idRegelsett, idTestregel) values (:idRegelsett, :idTestregel)"
    fun insertRegelsettTestregelParameters(idRegelsett: Int, idTestregel: Int) =
      MapSqlParameterSource("idRegelsett", idRegelsett)
      .addValue("idTestregel", idTestregel)


    val updateTestregelSql = ""
    val updateRegelsettSql = ""

    val deleteTestregelSql = "delete from testregel where id = :id"
    fun deleteTestregelParameters(id: Int) = MapSqlParameterSource("id", id)

    val deleteRegelsettSql = "delete from regelsett where id = :id"
    fun deleteRegelsettParameters(id: Int) = MapSqlParameterSource("id", id)
  }

  override fun listTestreglar(): List<Testregel> =
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
              Testregel(
              tr.testregelId,
              tr.testregelKravId,
              tr.testregelReferanseAct,
              tr.testregelKravTilSamsvar,
              tr.testregelType,
              tr.testregelStatus,
              tr.kravTittel
              )
            }
            .toList())
      }

  override fun createTestregel(testregel: Testregel): List<Testregel> {
    TODO("Not yet implemented")
  }

  @Transactional
  override fun createRegelsett(regelsettRequest: RegelsettRequest): List<Regelsett> {
    val keyHolder: KeyHolder = GeneratedKeyHolder()

    jdbcTemplate.update(insertRegelsett, insertRegelsettParameters(regelsettRequest.namn), keyHolder, arrayOf("id"))
    val regelsettId = keyHolder.key!!.toInt()

    for (id in regelsettRequest.ids) {
      jdbcTemplate.update(insertRegelsettTestregel, insertRegelsettTestregelParameters(regelsettId, id))
    }

    return jdbcTemplate
      .query(listRegelsettSql, regelsettListDAORowmapper)
      .groupBy { it.regelsettId to it.regelsettNamn }
      .map {
        Regelsett(
          it.key.first,
          it.key.second,
          it.value
            .map { tr ->
              Testregel(
                tr.testregelId,
                tr.testregelKravId,
                tr.testregelReferanseAct,
                tr.testregelKravTilSamsvar,
                tr.testregelType,
                tr.testregelStatus,
                tr.kravTittel
              )
            }
            .toList())
      }
  }

  override fun updateTestregel(testregel: Testregel): Testregel {
    TODO("Not yet implemented")
  }

  override fun updateRegelsett(regelsett: Regelsett): Regelsett {
    TODO("Not yet implemented")
  }

  @Transactional
  override fun deleteTestregel(id: Int) {
    jdbcTemplate
      .update(deleteTestregelSql, deleteTestregelParameters(id))
  }

  @Transactional
  override fun deleteRegelsett(id: Int) {
    jdbcTemplate
      .update(deleteRegelsettSql, deleteTestregelParameters(id))
  }
}
