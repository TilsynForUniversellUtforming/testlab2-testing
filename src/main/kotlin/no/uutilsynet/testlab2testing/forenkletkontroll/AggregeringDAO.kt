package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AggregeringDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val autoTesterClient: AutoTesterClient,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
    val testregelDAO: TestregelDAO,
) {

  private fun createAggregertResultatTestregel(
      aggregertResultatTestregel: AggregertResultatTestregel
  ) {

    val sql =
        "insert into aggregering_testregel(maaling_id,loeysing_id,suksesskriterium,fleire_suksesskriterium,testregel_id,tal_element_samsvar,tal_element_brot,tal_element_varsel,tal_element_ikkje_forekomst,tal_sider_samsvar,tal_sider_brot,tal_sider_ikkje_forekomst,testregel_gjennomsnittleg_side_brot_prosent,testregel_gjennomsnittleg_side_samsvar_prosent) " +
            "values(:maaling_id,:loeysing_id,:suksesskriterium,:fleire_suksesskriterium,:testregel_id,:tal_element_samsvar,:tal_element_brot,:tal_element_varsel,:tal_element_ikkje_forekomst,:tal_sider_samsvar,:tal_sider_brot,:tal_sider_ikkje_forekomst,:testregel_gjennomsnittleg_side_brot_prosent,:testregel_gjennomsnittleg_side_samsvar_prosent)"

    val parameterSource =
        MapSqlParameterSource()
            .addValue("maaling_id", aggregertResultatTestregel.maalingId, java.sql.Types.INTEGER)
            .addValue("loeysing_id", aggregertResultatTestregel.loeysing.id, java.sql.Types.INTEGER)
            .addValue(
                "suksesskriterium",
                aggregertResultatTestregel.suksesskriterium,
                java.sql.Types.VARCHAR)
            .addValue(
                "fleire_suksesskriterium",
                aggregertResultatTestregel.fleireSuksesskriterium.toTypedArray(),
                java.sql.Types.ARRAY)
            .addValue(
                "testregel_id",
                getTestregelIdFromSchema(aggregertResultatTestregel.testregelId),
                java.sql.Types.INTEGER)
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


    jdbcTemplate.update(sql, parameterSource)
  }

  @Transactional
  fun saveAggregertResultatTestregel(testKoeyring: TestKoeyring.Ferdig) {
    val aggregeringUrl: URI? = testKoeyring.lenker?.urlAggregeringTR?.toURI()

    if (aggregeringUrl != null) {
      val aggregertResultatTestregel: List<AggregertResultatTestregel> =
          autoTesterClient.fetchResultatAggregering(
              aggregeringUrl, AutoTesterClient.ResultatUrls.urlAggreggeringTR)
              as List<AggregertResultatTestregel>

      aggregertResultatTestregel.forEach { createAggregertResultatTestregel(it) }
    }
  }

  fun getAggregertResultatTestregelForMaaling(maalingId: Int): List<AggregertResultatTestregel> {
    val query = "select * from aggregering_testregel where maalingId = :maalingId"
    val params = mapOf("maalingId" to maalingId)
    return jdbcTemplate.query(query, params) { rs, _ ->
      AggregertResultatTestregel(
          maalingId = rs.getInt("maalingId"),
          testregelId = rs.getString("testregelId"),
          loeysing = getLoeysingFromId(rs.getInt("loeysingId")),
          suksesskriterium = rs.getString("suksesskriterium"),
          fleireSuksesskriterium = rs.getArray("fleiresuksesskriterium").array as List<String>,
          talElementSamsvar = rs.getInt("talelementsamsvar"),
          talElementBrot = rs.getInt("talelementbrot"),
          talElementVarsel = rs.getInt("talelementvarsel"),
          talElementIkkjeForekomst = rs.getInt("talelementikkjeforekomst"),
          talSiderSamsvar = rs.getInt("talsidersamsvar"),
          talSiderBrot = rs.getInt("talsiderbrot"),
          talSiderIkkjeForekomst = rs.getInt("talsiderikkjeforekomst"),
          testregelGjennomsnittlegSideBrotProsent =
              rs.getFloat("testregelgjennomsnittlegsidebrotprosent"),
          testregelGjennomsnittlegSideSamsvarProsent =
              rs.getFloat("testregelgjennomsnittlegsidesamsvarprosent"))
    }
  }

  fun getLoeysingFromId(loeysingId: Int): Loeysing {
    loeysingsRegisterClient.getMany(listOf(loeysingId)).let { loeysingList ->
      loeysingList.getOrThrow().firstOrNull()?.let { loeysing ->
        return loeysing
      }
    }
    throw RuntimeException("Fant ikke løsning med id $loeysingId")
  }

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    println("Testregelkey: $testregelKey")
    testregelDAO.getTestregelBySchema(testregelKey).let { testregel ->
      return testregel?.id
    }
  }
}
