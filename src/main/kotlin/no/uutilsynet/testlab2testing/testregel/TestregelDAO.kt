package no.uutilsynet.testlab2testing.testregel

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.uutilsynet.testlab2.constants.TestregelModus
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TestregelDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  object TestregelParams {

    val getTestregelListSql =
        """select id, testregel_id,versjon,namn, krav_id, status, dato_sist_endra,type , modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing  from "testlab2_testing"."testregel" order by id"""

    val getTestregelSql =
        """select id, testregel_id,versjon,namn, krav_id, status, dato_sist_endra,type, modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing from "testlab2_testing"."testregel" where id = :id order by id"""

    val getTestregelByTestregelId =
        """select id, testregel_id,versjon,namn, krav_id, status, dato_sist_endra,type, modus ,spraak,tema,testobjekt,krav_til_samsvar,testregel_schema, innhaldstype_testing from "testlab2_testing"."testregel" where testregel_id = :testregelId and versjon=(select max(versjon) from testlab2_testing.testregel where testregel_id= :testregelId) order by id limit 1"""

    val testregelRowMapper = DataClassRowMapper.newInstance(Testregel::class.java)

    val deleteTestregelSql = """delete from "testlab2_testing"."testregel" where id = :id"""

    val maalingTestregelListByIdSql =
        """select maaling_id from "testlab2_testing"."maaling_testregel" where testregel_id = :testregel_id"""

    val maalingTestregelSql =
        """
      select tr.id,tr.testregel_id,tr.versjon,tr.namn, tr.krav_id, tr.status, tr.dato_sist_endra,tr.type , tr.modus ,tr.spraak,tr.tema,tr.testobjekt,tr.krav_til_samsvar,tr.testregel_schema, tr.innhaldstype_testing
      from "testlab2_testing"."maalingv1" m
        join "testlab2_testing"."maaling_testregel" mt on m.id = mt.maaling_id
        join "testlab2_testing"."testregel" tr on mt.testregel_id = tr.id
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

  @Cacheable("testregelByTestregelId", unless = "#result==null")
  fun getTestregelByTestregelId(testregelId: String): Testregel? =
      DataAccessUtils.singleResult(
          jdbcTemplate.query(
              getTestregelByTestregelId, mapOf("testregelId" to testregelId), testregelRowMapper))

  fun getMany(testregelIdList: List<Int>): List<TestregelBase> =
      jdbcTemplate.query(
          """select tr.id, tr.namn, tr.krav_id, tr.modus, tr.type from "testlab2_testing"."testregel" tr where tr.id in (:ids)""",
          mapOf("ids" to testregelIdList),
          DataClassRowMapper.newInstance(TestregelBase::class.java))

  @Transactional
  @CacheEvict(
      value =
          [
              "testregel",
              "testregelByTestregelId",
              "testregelar",
              "regelsett",
              "regelsettlist",
              "regelsettlistbase"],
      allEntries = true)
  fun createTestregel(testregelInit: TestregelInit): Int {

    val keyHolder: KeyHolder = GeneratedKeyHolder()

    val params = MapSqlParameterSource()
    params.addValue("krav_id", testregelInit.kravId)
    params.addValue("testregel_schema", testregelInit.testregelSchema)
    params.addValue("namn", testregelInit.namn)
    params.addValue("modus", testregelInit.modus.value)
    params.addValue("testregel_id", setTestregelId(testregelInit))
    params.addValue("versjon", 1)
    params.addValue("status", testregelInit.status.value)
    params.addValue(
        "dato_sist_endra",
        Timestamp.from(testregelInit.datoSistEndra.truncatedTo(ChronoUnit.MINUTES)))
    params.addValue("spraak", testregelInit.spraak.value)
    params.addValue("tema", testregelInit.tema)
    params.addValue("type", testregelInit.type.value)
    params.addValue("testobjekt", testregelInit.testobjekt)
    params.addValue("krav_til_samsvar", testregelInit.kravTilSamsvar)
    params.addValue("innhaldstype_testing", testregelInit.innhaldstypeTesting)

    jdbcTemplate.update(
        """
          insert into 
            "testlab2_testing"."testregel"(
              krav_id,
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
              :krav_id,
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
        """
            .trimIndent(),
        params,
        keyHolder)

    return keyHolder.keys?.get("id") as Int
  }

  @Transactional
  @CacheEvict(
      cacheNames =
          [
              "testregel",
              "testregelByTestregelId",
              "testregelar",
              "regelsett",
              "regelsettlist",
              "regelsettlistbase"],
      allEntries = true)
  fun updateTestregel(testregel: Testregel) =
      jdbcTemplate.update(
          """ update "testlab2_testing"."testregel" set namn = :namn, testregel_id = :testregel_id,krav_id = :krav_id, versjon = :versjon,status = :status, dato_sist_endra = :dato_sist_endra, type = :type, modus = :modus,
              spraak = :spaak, tema = :tema, testobjekt = :testobjekt, krav_til_samsvar = :krav_til_samsvar , testregel_schema = :testregel_schema, innhaldstype_testing = :innhaldstype_testing where id = :id""",
          mapOf(
              "id" to testregel.id,
              "testregel_id" to testregel.testregelId,
              "versjon" to testregel.versjon,
              "namn" to testregel.namn,
              "krav_id" to testregel.kravId,
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
      cacheNames =
          [
              "testregel",
              "testregelByTestregelId",
              "testregelar",
              "regelsett",
              "regelsettlist",
              "regelsettlistbase"],
      allEntries = true)
  fun deleteTestregel(testregelId: Int) =
      jdbcTemplate.update(deleteTestregelSql, mapOf("id" to testregelId))

  fun getMaalingTestregelListById(testregelId: Int): List<Int> =
      jdbcTemplate.queryForList(
          maalingTestregelListByIdSql, mapOf("testregel_id" to testregelId), Int::class.java)

  fun getTestreglarForMaaling(maalingId: Int): Result<List<Testregel>> = runCatching {
    jdbcTemplate.query(maalingTestregelSql, mapOf("id" to maalingId), testregelRowMapper)
  }

  fun setTestregelId(testregelInit: TestregelInit): String {
    return if (testregelInit.modus == TestregelModus.automatisk) {
      testregelInit.testregelSchema
    } else testregelInit.testregelId
  }

  @Cacheable("innhaldstypeForTesting")
  fun getInnhaldstypeForTesting(): List<InnhaldstypeTesting> =
      jdbcTemplate.query(
          """select * from "testlab2_testing"."innhaldstype_testing"""",
          DataClassRowMapper.newInstance(InnhaldstypeTesting::class.java))

  fun getTemaForTestregel(): List<Tema> =
      jdbcTemplate.query(
          """select * from "testlab2_testing"."tema"""",
          DataClassRowMapper.newInstance(Tema::class.java))

  @Cacheable("testobjekt")
  fun getTestobjekt(): List<Testobjekt> =
      jdbcTemplate.query(
          """select * from "testlab2_testing"."testobjekt"""",
          DataClassRowMapper.newInstance(Testobjekt::class.java))

  fun getTestregelForKrav(kravId: Int): List<Testregel> =
      jdbcTemplate.query(
          """select * from "testlab2_testing"."testregel" where krav_id = :kravId""",
          mapOf("kravId" to kravId),
          testregelRowMapper)

  fun createInnholdstypeTesting(innholdstypeTesting: String): Int {
    return jdbcTemplate.update(
        """insert into "testlab2_testing"."innhaldstype_testing" (innhaldstype) values (:innhaldstype_testing)""",
        mapOf("innhaldstype_testing" to innholdstypeTesting))
  }

  fun createTema(tema: String): Int {
    val keyHolder = GeneratedKeyHolder()

    val params = MapSqlParameterSource()
    params.addValue("tema", tema)

    jdbcTemplate.update(
        """insert into "testlab2_testing"."tema" (tema) values (:tema)""", params, keyHolder)

    return keyHolder.keys?.get("id") as Int
  }
}
