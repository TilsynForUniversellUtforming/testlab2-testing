package no.uutilsynet.testlab2testing.aggregering

import java.net.URI
import java.sql.ResultSet
import java.util.*
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class AggregeringDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun createAggregertResultatTestregel(
      aggregertResultatTestregel: AggregeringPerTestregelDTO
  ): Int {

    val sql =
        "insert into aggregering_testregel(maaling_id,loeysing_id,suksesskriterium,fleire_suksesskriterium,testregel_id,tal_element_samsvar,tal_element_brot,tal_element_varsel,tal_element_ikkje_forekomst,tal_sider_samsvar,tal_sider_brot,tal_sider_ikkje_forekomst,testregel_gjennomsnittleg_side_brot_prosent,testregel_gjennomsnittleg_side_samsvar_prosent,testgrunnlag_id) " +
            "values(:maaling_id,:loeysing_id,:suksesskriterium,:fleire_suksesskriterium,:testregel_id,:tal_element_samsvar,:tal_element_brot,:tal_element_varsel,:tal_element_ikkje_forekomst,:tal_sider_samsvar,:tal_sider_brot,:tal_sider_ikkje_forekomst,:testregel_gjennomsnittleg_side_brot_prosent,:testregel_gjennomsnittleg_side_samsvar_prosent,:testgrunnlag_id)"

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

  fun createAggregertResultatSuksesskriterium(
      aggregertResultatSuksesskriterium: AggregeringPerSuksesskriteriumDTO
  ): Int {

    val sql =
        """insert into aggregering_suksesskriterium
            (maaling_id, loeysing_id, suksesskriterium_id, tal_sider_samsvar, tal_sider_brot, tal_sider_ikkje_forekomst)
            values(:maaling_id,:loeysing_id,:suksesskriterium,:tal_sider_samsvar,:tal_sider_brot,:tal_sider_ikkje_forekomst)"""

    val parameterMap =
        mapOf(
            "maaling_id" to aggregertResultatSuksesskriterium.maalingId,
            "loeysing_id" to aggregertResultatSuksesskriterium.loeysingId,
            "suksesskriterium" to aggregertResultatSuksesskriterium.suksesskriteriumId,
            "tal_sider_samsvar" to aggregertResultatSuksesskriterium.talSiderSamsvar,
            "tal_sider_brot" to aggregertResultatSuksesskriterium.talSiderBrot,
            "tal_sider_ikkje_forekomst" to aggregertResultatSuksesskriterium.talSiderIkkjeForekomst)

    return jdbcTemplate.update(sql, parameterMap)
  }

  fun createAggregeringSide(aggregertResultatSide: AggregeringPerSideDTO): Int {
    val sql =
        """insert into aggregering_side
              (maaling_id, loeysing_id, side,
   tal_element_samsvar,tal_element_brot,tal_element_ikkje_forekomst )
              values(:maaling_id,:loeysing_id,
   :side,:tal_element_samsvar,:tal_element_brot,:tal_element_ikkje_forekomst)"""

    val parameterMap =
        mapOf(
            "maaling_id" to aggregertResultatSide.maalingId,
            "loeysing_id" to aggregertResultatSide.loeysingId,
            "side" to aggregertResultatSide.sideUrl.toURI().toString(),
            "side_nivaa" to aggregertResultatSide.sideNivaa,
            "tal_element_samsvar" to aggregertResultatSide.talElementSamsvar,
            "tal_element_brot" to aggregertResultatSide.talElementBrot,
            "tal_element_varsel" to aggregertResultatSide.talElementVarsel,
            "tal_element_ikkje_forekomst" to aggregertResultatSide.talElementIkkjeForekomst)

    return jdbcTemplate.update(sql, parameterMap)
  }

  fun getAggregertResultatTestregelForMaaling(maalingId: Int): List<AggregeringPerTestregelDTO> {
    val query =
        "select * from aggregering_testregel where maaling_id = :maalingId order by loeysing_id"
    val params = mapOf("maalingId" to maalingId)
    return jdbcTemplate.query(query, params) { rs, _ -> aggregeringPerTestregelRowmapper(rs) }
  }

  fun getAggregertResultatSideForMaaling(maalingId: Int): List<AggregeringPerSideDTO> {
    val query = "select * from aggregering_side where maaling_id = :maalingId order by loeysing_id"
    val params = mapOf("maalingId" to maalingId)

    return jdbcTemplate.query(query, params) { rs, _ ->
      AggregeringPerSideDTO(
          maalingId = rs.getInt("maaling_id"),
          loeysingId = rs.getInt("loeysing_id"),
          sideUrl = URI(rs.getString("side")).toURL(),
          sideNivaa = rs.getInt("side_nivaa"),
          gjennomsnittligBruddProsentTR = rs.getFloat("gjennomsnittlig_brudd_prosent_tr"),
          talElementSamsvar = rs.getInt("tal_element_samsvar"),
          talElementBrot = rs.getInt("tal_element_brot"),
          talElementVarsel = rs.getInt("tal_element_varsel"),
          talElementIkkjeForekomst = rs.getInt("tal_element_ikkje_forekomst"))
    }
  }

  fun getAggregertResultatSuksesskriteriumForMaaling(
      maalingId: Int
  ): List<AggregeringPerSuksesskriteriumDTO> {
    val query =
        "select * from aggregering_suksesskriterium where maaling_id = :maalingId order by loeysing_id"
    val params = mapOf("maalingId" to maalingId)

    return jdbcTemplate.query(query, params) { rs, _ ->
      AggregeringPerSuksesskriteriumDTO(
          maalingId = rs.getInt("maaling_id"),
          loeysingId = rs.getInt("loeysing_id"),
          suksesskriteriumId = rs.getInt("suksesskriterium_id"),
          talSiderSamsvar = rs.getInt("tal_sider_samsvar"),
          talSiderBrot = rs.getInt("tal_sider_brot"),
          talSiderIkkjeForekomst = rs.getInt("tal_sider_ikkje_forekomst"))
    }
  }

  fun sqlArrayToList(sqlArray: java.sql.Array): List<Int> {
    val array = sqlArray.array as Array<Int>
    return Arrays.asList(*array)
  }

  fun harMaalingLagraAggregering(maalingId: Int, aggregeringstype: String): Boolean {

    val aggregeringsTabell =
        when (aggregeringstype) {
          "testresultat" -> "aggregering_testregel"
          "side" -> "aggregering_side"
          "suksesskriterium" -> "aggregering_suksesskriterium"
          else -> throw RuntimeException("Ugyldig aggregeringstype")
        }

    val queryString = "select count(*) from $aggregeringsTabell where maaling_id = :maalingId"

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
            select * from aggregering_testregel where testgrunnlag_id = :testgrunnlagId order by testregel_id
        """

    return jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
      aggregeringPerTestregelRowmapper(rs)
    }
  }

  private fun aggregeringPerTestregelRowmapper(rs: ResultSet) =
      AggregeringPerTestregelDTO(
          maalingId = rs.getInt("maaling_id"),
          testregelId = rs.getInt("testregel_id"),
          loeysingId = rs.getInt("loeysing_id"),
          suksesskriterium = rs.getInt("suksesskriterium"),
          fleireSuksesskriterium = sqlArrayToList(rs.getArray("fleire_suksesskriterium")),
          talElementSamsvar = rs.getInt("tal_element_samsvar"),
          talElementBrot = rs.getInt("tal_element_brot"),
          talElementVarsel = rs.getInt("tal_element_varsel"),
          talElementIkkjeForekomst = rs.getInt("tal_element_ikkje_forekomst"),
          talSiderSamsvar = rs.getInt("tal_sider_samsvar"),
          talSiderBrot = rs.getInt("tal_sider_brot"),
          talSiderIkkjeForekomst = rs.getInt("tal_sider_ikkje_forekomst"),
          testregelGjennomsnittlegSideBrotProsent =
              rs.getFloat("testregel_gjennomsnittleg_side_brot_prosent"),
          testregelGjennomsnittlegSideSamsvarProsent =
              rs.getFloat("testregel_gjennomsnittleg_side_samsvar_prosent"),
          testgrunnlagId = rs.getInt("testgrunnlag_id"))
}
