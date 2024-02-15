package no.uutilsynet.testlab2testing.testregel

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.deleteTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelByTestregelId
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelListSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.getTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelListByIdSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.maalingTestregelSql
import no.uutilsynet.testlab2testing.testregel.TestregelDAO.TestregelParams.testregelRowMapper
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestregelDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object TestregelParams {

    val getTestregelListSql =
        "select id, testregel_id,versjon,namn, krav, status, dato_sist_endra,type , modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing  from testregel order by id"

    val getTestregelSql =
        "select id, testregel_id,versjon,namn, krav, status, dato_sist_endra,type, modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing from testregel where id = :id order by id"

    val getTestregelByTestregelId =
        "select id, testregel_id,versjon,namn, krav, status, dato_sist_endra,type, modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing from testregel where testregel_id = :testregelId and versjon=(select max(versjon) from testlab2_testing.testregel where testregel_id= :testregelId) order by id limit 1"

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = "delete from testregel where id = :id"

    val maalingTestregelListByIdSql =
        "select maaling_id from maaling_testregel where testregel_id = :testregel_id"

    val maalingTestregelSql =
        """
      select tr.id,tr.testregel_id,tr.versjon,tr.namn, tr.krav, tr.status, tr.dato_sist_endra,tr.type , tr.modus ,tr.spraak,tr.tema,tr.testobjekt,tr.krav_til_samsvar,tr.testregel_schema, tr.innhaldstype_testing
      from MaalingV1 m
        join Maaling_Testregel mt on m.id = mt.maaling_id
        join Testregel tr on mt.testregel_id = tr.id
      where m.id = :id
    """
            .trimIndent()
  }

  @Cacheable("testregel", unless = "#result==null")
  fun getTestregel(id: Int): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(getTestregelSql, mapOf("id" to id), testregelRowMapper))

  @Cacheable("testregelar", unless = "#result.isEmpty()")
  fun getTestregelList(): List<Testregel> =
      jdbcTemplate.query(getTestregelListSql, testregelRowMapper)

  @Cacheable("testregelar", unless = "#result.isEmpty()")
  fun getTestregelListResponse(): List<TestregelBase> =
      getTestregelList().map { it.toTestregelBase() }

  @Cacheable("testregelByTestregelId", unless = "#result==null")
  fun getTestregelByTestregelId(testregelId: String): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              getTestregelByTestregelId, mapOf("testregelId" to testregelId), testregelRowMapper))

  @Transactional
  fun createTestregel(testregelInit: TestregelInit): Int =
      jdbcTemplate.queryForObject(
          """
          insert into 
            testregel (
              krav,
              testregel_schema,
              namn,
              modus,
              testregel_id,
              versjon,
              status,
              dato_sist_endra,
              spraak,
              tema,
              type,
              testobjekt,
              krav_til_samsvar,
              innhaldstype_testing
            ) values (
              :krav,
              :testregel_schema,
              :namn,
              :modus,
              :testregel_id,
              :versjon,
              :status,
              :dato_sist_endra,
              :spraak,
              :tema,
              :type,
              :testobjekt,
              :krav_til_samsvar,
              :innhaldstype_testing
            ) 
            returning id
        """
              .trimIndent(),
          mapOf(
              "krav" to testregelInit.krav,
              "testregel_schema" to testregelInit.testregelSchema,
              "namn" to testregelInit.namn,
              "modus" to testregelInit.modus.value,
              "testregel_id" to setTestregelId(testregelInit),
              "versjon" to 1,
              "status" to testregelInit.status.value,
              "dato_sist_endra" to
                  Timestamp.from(testregelInit.datoSistEndra.truncatedTo(ChronoUnit.MINUTES)),
              "spraak" to testregelInit.spraak.value,
              "tema" to testregelInit.tema,
              "type" to testregelInit.type.value,
              "testobjekt" to testregelInit.testobjekt,
              "krav_til_samsvar" to testregelInit.kravTilSamsvar,
              "innhaldstype_testing" to testregelInit.innhaldstypeTesting),
          Int::class.java)!!

  @Transactional
  @CacheEvict(
      key = "#testregel.id",
      cacheNames =
          [
              "testregel",
              "testregelByTestregelId",
              "testregelar",
              "regelsett",
              "regelsettlist",
              "regelsettlistbase"])
  fun updateTestregel(testregel: Testregel) =
      jdbcTemplate.update(
          " update testregel set namn = :namn, testregel_id = :testregel_id,krav = :krav, versjon = :versjon,status = :status, dato_sist_endra = :dato_sist_endra, type = :type, modus = :modus, " +
              "spraak = :spaak, tema = :tema, testobjekt = :testobjekt, krav_til_samsvar = :krav_til_samsvar , testregel_schema = :testregel_schema, innhaldstype_testing = :innhaldstype_testing where id = :id",
          mapOf(
              "id" to testregel.id,
              "testregel_id" to testregel.testregelId,
              "versjon" to testregel.versjon,
              "namn" to testregel.namn,
              "krav" to testregel.krav,
              "status" to testregel.status.value,
              "dato_sist_endra" to Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MINUTES)),
              "type" to testregel.type.value,
              "modus" to testregel.modus.value,
              "spaak" to testregel.spraak.value,
              "tema" to testregel.tema,
              "testobjekt" to testregel.testobjekt,
              "krav_til_samsvar" to testregel.kravTilSamsvar,
              "testregel_schema" to testregel.testregelSchema,
              "innhaldstype_testing" to (testregel.innhaldstypeTesting)))

  @Transactional
  @CacheEvict(
      key = "#testregelId",
      cacheNames =
          [
              "testregel",
              "testregelByTestregelId",
              "testregelar",
              "regelsett",
              "regelsettlist",
              "regelsettlistbase"])
  fun deleteTestregel(testregelId: Int) =
      jdbcTemplate.update(deleteTestregelSql, mapOf("id" to testregelId))

  fun getMaalingTestregelListById(testregelId: Int): List<Int> =
      jdbcTemplate.queryForList(
          maalingTestregelListByIdSql, mapOf("testregel_id" to testregelId), Int::class.java)

  fun getTestreglarForMaaling(maalingId: Int): Result<List<Testregel>> = runCatching {
    jdbcTemplate.query(maalingTestregelSql, mapOf("id" to maalingId), testregelRowMapper)
  }

  fun getTestregelResponseForMaaling(maalingId: Int): Result<List<TestregelBase>> = runCatching {
    getTestreglarForMaaling(maalingId).getOrThrow().map { it.toTestregelBase() }
  }

  fun getTestreglarBySak(sakId: Int): List<Testregel> =
      jdbcTemplate.query(
          """
          select t.id,t.testregel_id,t.versjon,t.namn, t.krav, t.status, t.dato_sist_endra,t.type , t.modus ,t.spraak,t.tema,t.testobjekt,t.krav_til_samsvar,t.testregel_schema,t.innhaldstype_testing
          from testregel t
          join sak_testregel st on t.id = st.testregel_id
          where st.sak_id = :sak_id
      """
              .trimIndent(),
          mapOf("sak_id" to sakId),
          DataClassRowMapper.newInstance(Testregel::class.java))

  fun setTestregelId(testregelInit: TestregelInit): String {
    return if (testregelInit.modus == TestregelModus.forenklet) {
      testregelInit.testregelSchema
    } else testregelInit.namn
  }

  fun getInnhaldstypeForTesting(): List<InnhaldstypeTesting> =
      jdbcTemplate.query(
          "select * from innhaldstype_testing",
          DataClassRowMapper.newInstance(InnhaldstypeTesting::class.java))

  fun getTemaForTestregel(): List<Tema> =
      jdbcTemplate.query("select * from tema", DataClassRowMapper.newInstance(Tema::class.java))

  fun getTestobjekt(): List<Testobjekt> =
      jdbcTemplate.query(
          "select * from testobjekt", DataClassRowMapper.newInstance(Testobjekt::class.java))
}
