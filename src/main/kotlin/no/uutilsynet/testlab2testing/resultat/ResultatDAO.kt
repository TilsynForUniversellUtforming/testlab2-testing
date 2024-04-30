package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.Testgrunnlag
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun getTestresultatMaaling(): List<ResultatLoeysing> {
    val query =
        """
            select maaling_id, navn,dato_start,loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot
            from aggregering_testregel  
            join testlab2_testing.maalingv1 on maalingv1.id = aggregering_testregel.maaling_id
        """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ ->
      val maalingId = rs.getInt("maaling_id")
      val navn = rs.getString("navn")
      val datoStart = handleDate(rs.getDate("dato_start"))
      val loeysingId = rs.getInt("loeysing_id")
      val testregelGjennomsnittlegSideSamsvarProsent =
          rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
      val talElementSamsvar = rs.getInt("tal_element_samsvar")
      val talElementBrot = rs.getInt("tal_element_brot")

      ResultatLoeysing(
          maalingId,
          navn,
          Kontroll.KontrollType.AutomatiskKontroll,
          Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
          datoStart,
          "testar",
          loeysingId,
          testregelGjennomsnittlegSideSamsvarProsent,
          talElementSamsvar,
          talElementBrot)
    }
  }

  fun getTestresultatTestgrunnlag(): List<ResultatLoeysing> {
    val query =
        """
            select testgrunnlag_id, namn,dato_oppretta,loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot
            from aggregering_testregel  
            join testlab2_testing.testgrunnlag on testgrunnlag.id = aggregering_testregel.testgrunnlag_id
        """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ ->
      val testgrunnlag_id = rs.getInt("testgrunnlag_id")
      val namn = rs.getString("namn")
      val datoOppretta = handleDate(rs.getDate("dato_oppretta"))
      val loeysingId = rs.getInt("loeysing_id")
      val testregelGjennomsnittlegSideSamsvarProsent =
          rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
      val talElementSamsvar = rs.getInt("tal_element_samsvar")
      val talElementBrot = rs.getInt("tal_element_brot")

      ResultatLoeysing(
          testgrunnlag_id,
          namn,
          Kontroll.KontrollType.ManuellKontroll,
          Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
          datoOppretta,
          "testar",
          loeysingId,
          testregelGjennomsnittlegSideSamsvarProsent,
          talElementSamsvar,
          talElementBrot)
    }
  }

  fun getResultat(): List<ResultatLoeysing> {
    val query =
        """
                select (case when maaling_id is not null then maaling_id else testgrunnlag_id end) as id, 
                (case when maaling_id is not null then 'AutomatiskKontroll' else 'ManuellKontroll' end) as type_kontroll, 
                (case when dato_start is not null then dato_start else dato_oppretta end) as dato,
                (case when maaling_id is not null then navn else namn end) as namn,
                loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot
                from aggregering_testregel  
                left join testlab2_testing.maalingv1 on maalingv1.id = aggregering_testregel.maaling_id
                left join testlab2_testing.testgrunnlag on testgrunnlag.id = aggregering_testregel.testgrunnlag_id
            """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ ->
      val id = rs.getInt("id")
      val namn = rs.getString("namn") ?: ""
      val dato = handleDate(rs.getDate("dato"))
      val kontrollType = rs.getString("type_kontroll")
      val loeysingId = rs.getInt("loeysing_id")
      val testregelGjennomsnittlegSideSamsvarProsent =
          rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
      val talElementSamsvar = rs.getInt("tal_element_samsvar")
      val talElementBrot = rs.getInt("tal_element_brot")

      ResultatLoeysing(
          id,
          namn,
          Kontroll.KontrollType.valueOf(kontrollType),
          Testgrunnlag.TestgrunnlagType.OPPRINNELEG_TEST,
          dato,
          "testar",
          loeysingId,
          testregelGjennomsnittlegSideSamsvarProsent,
          talElementSamsvar,
          talElementBrot)
    }
  }

    fun handleDate(date: java.sql.Date?) : LocalDate  {
        if(date!=null) {
            return date.toLocalDate()
        }
        return LocalDate.now()
    }
}
