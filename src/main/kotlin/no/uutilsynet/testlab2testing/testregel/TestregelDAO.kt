package no.uutilsynet.testlab2testing.testregel

import java.time.LocalDate
import no.uutilsynet.testlab2testing.common.TestlabLocale
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelByTestregelId
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
        "select id, namn, krav, testregel_schema, modus  from testregel order by id"

    val getTestregelSql =
        "select id, namn, krav, testregel_schema, modus from testregel where id = :id order by id"

    val getTestregelByTestregelId =
        "select id, namn, krav, testregel_schema, modus from testregel where testregel_id = :testregelId and versjon=(select max(versjon) from testlab2_testing.testregel where testregel_id= :testregelId) order by id"

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = "delete from testregel where id = :id"

    val maalingTestregelListByIdSql =
        "select maaling_id from maaling_testregel where testregel_id = :testregel_id"

    val maalingTestregelSql =
        """
      select 
      tr.id,
      tr.namn,
      tr.krav,
      tr.testregel_schema,
      tr.modus
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

  fun getTestregelByTestregelId(testregelId: String): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              getTestregelByTestregelId,
              mapOf("testregelId" to testregelId),
              testregelRowMapper))

  @Transactional
  fun createTestregel(testregelInit: TestregelInit): Int =
      jdbcTemplate.queryForObject(
          "insert into testregel (namn, testregel_schema, modus, krav) values (:name, :testregel_schema, :modus, :krav) returning id",
          mapOf(
              "name" to testregelInit.name,
              "testregel_schema" to testregelInit.testregelSchema,
              "modus" to testregelInit.type.value,
              "krav" to testregelInit.krav),
          Int::class.java)!!

  @Transactional
  fun createAutomatiskTestregel(testregelInit: TestregelInitAutomatisk): Int =
      jdbcTemplate.queryForObject(
          """insert into testregel (testregel_id,versjon, namn, krav, status,dato_sist_endra, type,modus, spraak, tema,testobjekt,krav_til_samsvar, testregel_schema) values 
                  (:testregelId,:versjon, :namn, :krav, :status,:datoSistEndra, :type,:modus, :spraak, :tema,:testobjekt,:kravTilSamsvar, :testregelSchema) returning id""",
          mapOf(
              "testregelId" to testregelInit.testregelId,
              "versjon" to 1,
              "namn" to testregelInit.namn,
              "krav" to testregelInit.krav,
              "status" to TestregelStatus.publisert.value,
              "datoSistEndra" to LocalDate.now(),
              "innholdstype" to TestregelInnholdstype.nett.value,
              "type" to TestregelType.forenklet.value,
              "spraak" to TestlabLocale.nb.value,
              "tema" to testregelInit.tema,
              "testobjekt" to testregelInit.testobjekt,
              "testregel_schema" to testregelInit.testregelId),
          Int::class.java)!!

  fun createManuellTestregel(testregelInitManuell: TestregelInitManuell): Int =
      jdbcTemplate.queryForObject(
          """insert into testregel (testregel_id,versjon, namn, krav, status,dato_sist_endra, modus,type, spraak, tema,testobjekt,krav_til_samsvar, testregel_schema) values 
                  (:testregelId,:versjon, :namn, :krav, :status,:datoSistEndra, :modus,:type, :spraak, :tema,:testobjekt,:kravTilSamsvar, :testregelSchema) returning id""",
          mapOf(
              "testregelId" to testregelInitManuell.testregelId,
              "versjon" to 1,
              "namn" to testregelInitManuell.namn,
              "krav" to testregelInitManuell.krav,
              "status" to TestregelStatus.publisert.value,
              "datoSistEndra" to LocalDate.now(),
              "innholdstype" to TestregelInnholdstype.nett.value,
              "type" to TestregelType.inngaaende.value,
              "spraak" to TestlabLocale.nb.value,
              "tema" to testregelInitManuell.tema,
              "testobjekt" to testregelInitManuell.testobjekt,
              "testregel_schema" to testregelInitManuell.testregelId),
          Int::class.java)!!

  @Transactional
  fun updateTestregel(testregel: Testregel) =
      jdbcTemplate.update(
          " update testregel set namn = :name, krav = :krav, testregel_schema = :testregel_schema where id = :id",
          mapOf(
              "id" to testregel.id,
              "name" to testregel.namn,
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

  fun getTestreglarBySak(sakId: Int): List<Testregel> =
      jdbcTemplate.query(
          """
          select t.id, t.name, t.krav, t.testregel_schema, t.type
          from testregel t
          join sak_testregel st on t.id = st.testregel_id
          where st.sak_id = :sak_id
      """
              .trimIndent(),
          mapOf("sak_id" to sakId),
          DataClassRowMapper.newInstance(Testregel::class.java))
}
