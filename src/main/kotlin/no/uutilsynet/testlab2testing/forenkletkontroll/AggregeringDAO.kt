package no.uutilsynet.testlab2testing.forenkletkontroll

import java.net.URI
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.loeysing.LoeysingsRegisterClient
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AggregeringDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val autoTesterClient: AutoTesterClient,
    val loeysingsRegisterClient: LoeysingsRegisterClient,
) {

  private fun createAggregertResultatTestregel(
      aggregertResultatTestregel: AggregertResultatTestregel
  ) {

    val testregelId = 1

    val updateValuesAggregeringTestregel =
        mapOf(
            "maalingid" to aggregertResultatTestregel.maalingId,
            "loeysingid" to aggregertResultatTestregel.loeysing.id,
            "suksesskriterium" to aggregertResultatTestregel.suksesskriterium,
            "fleiresuksesskriterium" to aggregertResultatTestregel.fleireSuksesskriterium,
            "testregelid" to testregelId,
            "talelementsamsvar" to aggregertResultatTestregel.talElementSamsvar,
            "talelementbrot" to aggregertResultatTestregel.talElementBrot,
            "talelementvarsel" to aggregertResultatTestregel.talElementVarsel,
            "talelementikkjeforekomst" to aggregertResultatTestregel.talElementIkkjeForekomst,
            "talsidersamsvar" to aggregertResultatTestregel.talSiderSamsvar,
            "talsiderbrot" to aggregertResultatTestregel.talSiderBrot,
            "talsiderikkjeforekomst" to aggregertResultatTestregel.talSiderIkkjeForekomst,
            "testregelgjennomsnittlegsidebrotprosent" to
                aggregertResultatTestregel.testregelGjennomsnittlegSideBrotProsent,
            "testregelgjennomsnittlegsidesamsvarprosent" to
                aggregertResultatTestregel.testregelGjennomsnittlegSideSamsvarProsent)

    jdbcTemplate.update(
        "insert into aggregering_testregel(maalingid,loeysingid,suksesskriterium,fleiresuksesskriterium,testregelid,talelementsamsvar,talelementbrot,talelementvarsel,talelementikkjeforekomst,talsidersamsvar,talsiderbrot,talsiderikkjeforekomst,testregelgjennomsnittlegsidebrotprosent,testregelgjennomsnittlegsidesamsvarprosent) " +
                                      "values(:maalingid,:loeysingid,:suksesskriterium,array [:fleiresuksesskriterium],:testregelid,:talelementsamsvar,:talelementbrot,:talelementvarsel,:talelementikkjeforekomst,:talsidersamsvar,:talsiderbrot,:talsiderikkjeforekomst,:testregelgjennomsnittlegsidebrotprosent,:testregelgjennomsnittlegsidesamsvarprosent)",
        updateValuesAggregeringTestregel)
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
}
