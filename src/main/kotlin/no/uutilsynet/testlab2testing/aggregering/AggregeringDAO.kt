package no.uutilsynet.testlab2testing.aggregering

import java.net.URI
import java.sql.ResultSet
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class AggregeringDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun createAggregertResultatTestregel(
      aggregertResultatTestregel: AggregeringPerTestregelDTO
  ): Int {

    deleteAggregertResultatTestregel(aggregertResultatTestregel)

    val sql =
        """insert into "testlab2_testing"."aggregering_testregel"(maaling_id,loeysing_id,suksesskriterium,fleire_suksesskriterium,testregel_id,tal_element_samsvar,tal_element_brot,tal_element_varsel,tal_element_ikkje_forekomst,tal_sider_samsvar,tal_sider_brot,tal_sider_ikkje_forekomst,testregel_gjennomsnittleg_side_brot_prosent,testregel_gjennomsnittleg_side_samsvar_prosent,testgrunnlag_id)
            values(:maaling_id,:loeysing_id,:suksesskriterium,:fleire_suksesskriterium,:testregel_id,:tal_element_samsvar,:tal_element_brot,:tal_element_varsel,:tal_element_ikkje_forekomst,:tal_sider_samsvar,:tal_sider_brot,:tal_sider_ikkje_forekomst,:testregel_gjennomsnittleg_side_brot_prosent,:testregel_gjennomsnittleg_side_samsvar_prosent,:testgrunnlag_id)"""

    val parameterSource =
        MapSqlParameterSource()
            .addValue("maaling_id", aggregertResultatTestregel.maalingId, java.sql.Types.INTEGER)
            .addValue("loeysing_id", aggregertResultatTestregel.loeysingId, java.sql.Types.INTEGER)
            .addValue(
                "suksesskriterium",
                aggregertResultatTestregel.suksesskriterium,
                java.sql.Types.INTEGER)
            .addValue(
                "fleire_suksesskriterium",
                aggregertResultatTestregel.fleireSuksesskriterium.map { it }.toTypedArray(),
                java.sql.Types.ARRAY)
            .addValue(
                "testregel_id", aggregertResultatTestregel.testregelId, java.sql.Types.INTEGER)
            .addValue(
                "tal_element_samsvar",
                aggregertResultatTestregel.talElementSamsvar,
                java.sql.Types.INTEGER)
            .addValue(
                "tal_element_brot",
                aggregertResultatTestregel.talElementBrot,
                java.sql.Types.INTEGER)
            .addValue(
                "tal_element_varsel",
                aggregertResultatTestregel.talElementVarsel,
                java.sql.Types.INTEGER)
            .addValue(
                "tal_element_ikkje_forekomst",
                aggregertResultatTestregel.talElementIkkjeForekomst,
                java.sql.Types.INTEGER)
            .addValue(
                "tal_sider_samsvar",
                aggregertResultatTestregel.talSiderSamsvar,
                java.sql.Types.INTEGER)
            .addValue(
                "tal_sider_brot", aggregertResultatTestregel.talSiderBrot, java.sql.Types.INTEGER)
            .addValue(
                "tal_sider_ikkje_forekomst",
                aggregertResultatTestregel.talSiderIkkjeForekomst,
                java.sql.Types.INTEGER)
            .addValue(
                "testregel_gjennomsnittleg_side_brot_prosent",
                aggregertResultatTestregel.testregelGjennomsnittlegSideBrotProsent,
                java.sql.Types.FLOAT)
            .addValue(
                "testregel_gjennomsnittleg_side_samsvar_prosent",
                aggregertResultatTestregel.testregelGjennomsnittlegSideSamsvarProsent,
                java.sql.Types.FLOAT)
            .addValue(
                "testgrunnlag_id",
                aggregertResultatTestregel.testgrunnlagId,
                java.sql.Types.INTEGER)

    return jdbcTemplate.update(sql, parameterSource)
  }

  fun floatNullVedIkkjeForekomst(
      value: Double,
      talElementSamsvar: Int,
      talElemenBrot: Int
  ): Double? {
    if (talElemenBrot == 0 && talElementSamsvar == 0) {
      return null
    }
    return value
  }

  fun createAggregertResultatSuksesskriterium(
      aggregertResultatSuksesskriterium: AggregeringPerSuksesskriteriumDTO
  ): Int {

    deleteAggregertResultatSuksesskriterium(aggregertResultatSuksesskriterium)

    val sql =
        """insert into "testlab2_testing"."aggregering_suksesskriterium"
            (maaling_id, loeysing_id, suksesskriterium_id, tal_sider_samsvar, tal_sider_brot, tal_sider_ikkje_forekomst, testgrunnlag_id)
            values(:maaling_id,:loeysing_id,:suksesskriterium,:tal_sider_samsvar,:tal_sider_brot,:tal_sider_ikkje_forekomst, :testgrunnlag_id)"""

    val parameterMap =
        mapOf(
            "maaling_id" to aggregertResultatSuksesskriterium.maalingId,
            "loeysing_id" to aggregertResultatSuksesskriterium.loeysingId,
            "suksesskriterium" to aggregertResultatSuksesskriterium.suksesskriteriumId,
            "tal_sider_samsvar" to aggregertResultatSuksesskriterium.talSiderSamsvar,
            "tal_sider_brot" to aggregertResultatSuksesskriterium.talSiderBrot,
            "tal_sider_ikkje_forekomst" to aggregertResultatSuksesskriterium.talSiderIkkjeForekomst,
            "testgrunnlag_id" to aggregertResultatSuksesskriterium.testgrunnlagId)

    return jdbcTemplate.update(sql, parameterMap)
  }

  fun createAggregeringSide(aggregertResultatSide: AggregeringPerSideDTO): Result<Int> {

    deleteAggregertResultatSide(aggregertResultatSide)
    val sql =
        """insert into "testlab2_testing"."aggregering_side"
              (maaling_id, loeysing_id, side,
   tal_element_samsvar,tal_element_brot,tal_element_ikkje_forekomst,testgrunnlag_id )
              values(:maaling_id,:loeysing_id,
   :side,:tal_element_samsvar,:tal_element_brot,:tal_element_ikkje_forekomst, :testgrunnlag_id)"""

    val parameterMap =
        mapOf(
            "maaling_id" to aggregertResultatSide.maalingId,
            "loeysing_id" to aggregertResultatSide.loeysingId,
            "side" to aggregertResultatSide.sideUrl.toURI().toString(),
            "side_nivaa" to aggregertResultatSide.sideNivaa,
            "tal_element_samsvar" to aggregertResultatSide.talElementSamsvar,
            "tal_element_brot" to aggregertResultatSide.talElementBrot,
            "tal_element_varsel" to aggregertResultatSide.talElementVarsel,
            "tal_element_ikkje_forekomst" to aggregertResultatSide.talElementIkkjeForekomst,
            "testgrunnlag_id" to aggregertResultatSide.testgrunnlagId)

    val result = jdbcTemplate.update(sql, parameterMap)
    if (result < 1) {
      return Result.failure(
          RuntimeException(
              "Kunne ikkje lagre aggregert resultat for side for testgrunnlag ${aggregertResultatSide.testgrunnlagId} og side ${aggregertResultatSide.sideUrl.toURI().toString()}"))
    }
    return Result.success(result)
  }

  fun deleteAggregertResultatTestregel(aggregering: AggregeringPerTestregelDTO): Int {
    val sql =
        """delete from "testlab2_testing"."aggregering_testregel" where testregel_id = :testregelId and  loeysing_id=:loeysingId and (maaling_id=:maalingId or testgrunnlag_id=:testgrunnlagId)"""
    return jdbcTemplate.update(
        sql,
        mapOf(
            "testregelId" to aggregering.testregelId,
            "maalingId" to aggregering.maalingId,
            "loeysingId" to aggregering.loeysingId,
            "testgrunnlagId" to aggregering.testgrunnlagId))
  }

  fun deleteAggregertResultatSuksesskriterium(aggregering: AggregeringPerSuksesskriteriumDTO): Int {
    val sql =
        """delete from "testlab2_testing"."aggregering_suksesskriterium" where suksesskriterium_id=:suksesskriteriumId and loeysing_id=:loeysingId and (maaling_id=:maalingId or testgrunnlag_id=:testgrunnlagId)"""
    return jdbcTemplate.update(
        sql,
        mapOf(
            "suksesskriteriumId" to aggregering.suksesskriteriumId,
            "maalingId" to aggregering.maalingId,
            "loeysingId" to aggregering.loeysingId,
            "testgrunnlagId" to aggregering.testgrunnlagId))
  }

  fun deleteAggregertResultatSide(aggregering: AggregeringPerSideDTO): Int {
    val sql =
        """delete from "testlab2_testing"."aggregering_side" where side=:side and loeysing_id=:loeysingId and (maaling_id=:maalingId or testgrunnlag_id=:testgrunnlagId)"""
    return jdbcTemplate.update(
        sql,
        mapOf(
            "side" to aggregering.sideUrl.toURI().toString(),
            "maalingId" to aggregering.maalingId,
            "loeysingId" to aggregering.loeysingId,
            "testgrunnlagId" to aggregering.testgrunnlagId))
  }

  fun getAggregertResultatTestregelForMaaling(maalingId: Int): List<AggregeringPerTestregelDTO> {
    val query =
        """select * from "testlab2_testing"."aggregering_testregel" where maaling_id = :maalingId order by loeysing_id"""
    val params = mapOf("maalingId" to maalingId)
    return jdbcTemplate.query(query, params) { rs, _ -> aggregeringPerTestregelRowmapper(rs) }
  }

  fun getAggregertResultatSideForMaaling(maalingId: Int): List<AggregeringPerSideDTO> {
    val query =
        """select * from "testlab2_testing"."aggregering_side" where maaling_id = :maalingId order by loeysing_id"""
    val params = mapOf("maalingId" to maalingId)

    return jdbcTemplate.query(query, params) { rs, _ -> aggregeringPerSideRowmapper(rs) }
  }

  fun getAggregertResultatSuksesskriteriumForMaaling(
      maalingId: Int
  ): List<AggregeringPerSuksesskriteriumDTO> {
    val query =
        """select * from "testlab2_testing"."aggregering_suksesskriterium" where maaling_id = :maalingId order by loeysing_id"""
    val params = mapOf("maalingId" to maalingId)

    return jdbcTemplate.query(query, params) { rs, _ ->
      aggregeringPerSuksesskriteriumRowmapper(rs)
    }
  }

  fun sqlArrayToList(sqlArray: String): List<Int> {
    val kravIds = sqlArray.replace("[", "").replace("]", "").split(",").map { it.trim().toInt() }
    return kravIds
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {

    val aggregeringsTabell =
        when (aggregeringstype) {
          "testresultat" -> "aggregering_testregel"
          "side" -> "aggregering_side"
          "suksesskriterium" -> "aggregering_suksesskriterium"
          else -> throw RuntimeException("Ugyldig aggregeringstype")
        }

    val queryString =
        """select count(*) from "testlab2_testing.$aggregeringsTabell" where maaling_id = :maalingId"""

    val count =
        jdbcTemplate.queryForObject(
            queryString,
            mapOf("maalingId" to maalingId, "aggregeringstype" to aggregeringsTabell),
            Int::class.java)

    if (count != null && count > 0) {
      return true
    }
    return false
  }

  fun getAggregertResultatTestregelForTestgrunnlag(
      testgrunnlagId: Int
  ): List<AggregeringPerTestregelDTO> {
    val query =
        """
            select * from "testlab2_testing"."aggregering_testregel" where testgrunnlag_id = :testgrunnlagId order by testregel_id
        """

    return jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
      aggregeringPerTestregelRowmapper(rs)
    }
  }

  fun getAggregertResultatSideForTestgrunnlag(testgrunnlagId: Int): List<AggregeringPerSideDTO> {
    val query =
        """
            select * from "testlab2_testing"."aggregering_side" where testgrunnlag_id = :testgrunnlagId
        """

    return jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
      aggregeringPerSideRowmapper(rs)
    }
  }

  fun getAggregertResultatSuksesskriteriumForTestgrunnlag(
      testgrunnlagId: Int
  ): List<AggregeringPerSuksesskriteriumDTO> {
    val query =
        """
            select * from "testlab2_testing"."aggregering_suksesskriterium" where testgrunnlag_id = :testgrunnlagId
        """

    return jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
      aggregeringPerSuksesskriteriumRowmapper(rs)
    }
  }

  private fun aggregeringPerTestregelRowmapper(rs: ResultSet): AggregeringPerTestregelDTO {
    val talElementSamsvar = rs.getInt("tal_element_samsvar")
    val talElemenBrot = rs.getInt("tal_element_brot")

    return AggregeringPerTestregelDTO(
        maalingId = rs.getInt("maaling_id").takeIf { it > 0 },
        testregelId = rs.getInt("testregel_id"),
        loeysingId = rs.getInt("loeysing_id"),
        suksesskriterium = rs.getInt("suksesskriterium"),
        fleireSuksesskriterium = sqlArrayToList(rs.getString("fleire_suksesskriterium")),
        talElementSamsvar = rs.getInt("tal_element_samsvar"),
        talElementBrot = rs.getInt("tal_element_brot"),
        talElementVarsel = rs.getInt("tal_element_varsel"),
        talElementIkkjeForekomst = rs.getInt("tal_element_ikkje_forekomst"),
        talSiderSamsvar = rs.getInt("tal_sider_samsvar"),
        talSiderBrot = rs.getInt("tal_sider_brot"),
        talSiderIkkjeForekomst = rs.getInt("tal_sider_ikkje_forekomst"),
        testregelGjennomsnittlegSideBrotProsent =
            floatNullVedIkkjeForekomst(
                rs.getDouble("testregel_gjennomsnittleg_side_brot_prosent"),
                talElementSamsvar,
                talElemenBrot),
        testregelGjennomsnittlegSideSamsvarProsent =
            floatNullVedIkkjeForekomst(
                rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent"),
                talElementSamsvar,
                talElemenBrot),
        testgrunnlagId = rs.getInt("testgrunnlag_id").takeIf { it > 0 })
  }

  private fun aggregeringPerSideRowmapper(rs: ResultSet): AggregeringPerSideDTO {
    val talElementSamsvar = rs.getInt("tal_element_samsvar")
    val talElemenBrot = rs.getInt("tal_element_brot")

    return AggregeringPerSideDTO(
        maalingId = rs.getInt("maaling_id").takeIf { it > 0 },
        loeysingId = rs.getInt("loeysing_id"),
        sideUrl = URI(rs.getString("side")).toURL(),
        sideNivaa = rs.getInt("side_nivaa"),
        gjennomsnittligBruddProsentTR =
            floatNullVedIkkjeForekomst(
                rs.getDouble("gjennomsnittlig_brudd_prosent_tr"), talElementSamsvar, talElemenBrot),
        talElementSamsvar = rs.getInt("tal_element_samsvar"),
        talElementBrot = rs.getInt("tal_element_brot"),
        talElementVarsel = rs.getInt("tal_element_varsel"),
        talElementIkkjeForekomst = rs.getInt("tal_element_ikkje_forekomst"),
        testgrunnlagId = rs.getInt("testgrunnlag_id"))
  }

  private fun aggregeringPerSuksesskriteriumRowmapper(rs: ResultSet) =
      AggregeringPerSuksesskriteriumDTO(
          maalingId = rs.getInt("maaling_id").takeIf { it > 0 },
          loeysingId = rs.getInt("loeysing_id"),
          suksesskriteriumId = rs.getInt("suksesskriterium_id"),
          talSiderSamsvar = rs.getInt("tal_sider_samsvar"),
          talSiderBrot = rs.getInt("tal_sider_brot"),
          talSiderIkkjeForekomst = rs.getInt("tal_sider_ikkje_forekomst"),
          testgrunnlagId = rs.getInt("testgrunnlag_id"))
}
