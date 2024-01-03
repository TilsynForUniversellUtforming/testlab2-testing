package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import java.util.*
import no.uutilsynet.testlab2testing.krav.KravregisterClient
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
    val kravregisterClient: KravregisterClient,
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
                getKravFromSuksesskritterium(aggregertResultatTestregel.suksesskriterium),
                java.sql.Types.INTEGER)
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
    val query = "select * from aggregering_testregel where maaling_id = :maalingId"
    val params = mapOf("maalingId" to maalingId)
    return jdbcTemplate.query(query, params) { rs, _ ->
      AggregertResultatTestregel(
          maalingId = rs.getInt("maaling_id"),
          testregelId = getTestregelId(rs.getInt("testregel_id")),
          loeysing = getLoeysingFromId(rs.getInt("loeysing_id")),
          suksesskriterium = getSuksesskriteriumFromKrav(rs.getInt("suksesskriterium")),
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
              rs.getFloat("testregel_gjennomsnittleg_side_samsvar_prosent"))
    }
  }

  fun getLoeysingFromId(loeysingId: Int): Loeysing {
    loeysingsRegisterClient.getMany(listOf(loeysingId)).let { loeysingList ->
      loeysingList.getOrThrow().firstOrNull()?.let { loeysing ->
        return loeysing
      }
    }
    throw RuntimeException("Fant ikkje løsning med id $loeysingId")
  }

  fun sqlArrayToList(sqlArray: java.sql.Array): List<String> {
    val array = Arrays.asList(sqlArray.getArray())
    return array.map { it.toString() }
  }

  fun getTestregelIdFromSchema(testregelKey: String): Int? {
    testregelDAO.getTestregelByTestregelId(testregelKey).let { testregel ->
      return testregel?.id
    }
  }

  fun getKravFromSuksesskritterium(suksesskriterium: String): Int {
    kravregisterClient.getKrav(suksesskriterium).let { krav ->
      return krav.getOrThrow().id
    }
  }

  fun getSuksesskriteriumFromKrav(kravId: Int): String {
    kravregisterClient.getWcagKrav(kravId).let { krav ->
      return krav.getOrThrow().suksesskriterium
    }
  }

  fun getTestregelId(idTestregel: Int): String {
    testregelDAO.getTestregel(idTestregel).let { testregel ->
      return testregel?.testregelId.toString()
    }
  }
}
