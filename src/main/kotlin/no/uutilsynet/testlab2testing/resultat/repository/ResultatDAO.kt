package no.uutilsynet.testlab2testing.resultat.repository

import io.micrometer.observation.annotation.Observed
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2.constants.TestgrunnlagType
import no.uutilsynet.testlab2testing.resultat.ResultatMetadata
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.resultat.common.handleDate
import no.uutilsynet.testlab2testing.resultat.common.resultatLoeysingRowmapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  private val logger = LoggerFactory.getLogger(ResultatDAO::class.java)

  private val resultatQuery =
      """
        select 
          k.id     as id,
          k.tittel as tittel,
         coalesce(maaling_id, testgrunnlag_id) as testgrunnlag_id,
         testtype,
          kontrolltype,
          loeysing_id,
          testregel_gjennomsnittleg_side_samsvar_prosent,
          tal_element_samsvar,
          tal_element_brot,
          kontroll_id,
          dato,
          testregel_id,
          testrun_uuid
        from "testlab2_testing"."kontroll" k
          join (
            select 
              loeysing_id,
              testregel_gjennomsnittleg_side_samsvar_prosent,
              tal_element_samsvar,
              tal_element_brot,
              testregel_id,
              maaling_id,
              testgrunnlag_id,
              type as testtype,
              coalesce(m.uuid, t.uuid) as testrun_uuid,
              case
              when maaling_id is not null
                then m.kontrollid
                else t.kontroll_id
              end as kontroll_id,
              case
              when maaling_id is not null
                then m.dato_start
                else t.dato_oppretta
              end as dato
            from "testlab2_testing"."aggregering_testregel" agt
              left join "testlab2_testing"."maalingv1" m on m.id = agt.maaling_id
              left join "testlab2_testing"."testgrunnlag" t on t.id = agt.testgrunnlag_id
            where case
              when maaling_id is not null
                then m.kontrollid
                else t.kontroll_id
              end is not null
            ) as ag
        on k.id = ag.kontroll_id
      """
          .trimIndent()

  private val queryTestresultatMaaling =
      """select k.id, k.tittel as tittel,kontrolltype, maaling_id as testgrunnlag_id, 'OPPRINNELIG_TEST' as testtype,
        dato_start as dato,
        loeysing_id, testregel_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot
        from "testlab2_testing"."kontroll" k
        left join "testlab2_testing"."maalingv1" m on m.kontrollid=k.id
        join "testlab2_testing"."aggregering_testregel" agt on agt.maaling_id=m.id"""
          .trimIndent()

  private val queryTestresultatTestgrunnlag =
      """select k.id as id, k.tittel as tittel, kontrolltype, testgrunnlag_id, testregel_id, type as testtype, loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot,
            dato_oppretta as dato
            from "testlab2_testing"."kontroll" k
            left join "testlab2_testing"."testgrunnlag" t on t.kontroll_id=k.id
            join "testlab2_testing"."aggregering_testregel" agt on agt.testgrunnlag_id=t.id
        """
          .trimIndent()

  fun getTestresultatMaaling(): List<ResultatPerTestregelDTO> {
    val query = queryTestresultatMaaling.trimIndent()
    return jdbcTemplate.query(query) { rs, _ -> resultatLoeysingRowmapper(rs) }
  }

  fun getTestresultatMaaling(maalingId: Int): List<ResultatPerTestregelDTO> {
    val query = "$queryTestresultatMaaling where maaling_id = :maalingId"
    return jdbcTemplate.query(query, mapOf("maalingId" to maalingId)) { rs, _ ->
      resultatLoeysingRowmapper(rs)
    }
  }

    fun getResultatMetadata(kontrollId: Int, loeysingId: Int?): List<ResultatMetadata> {
        var query = """select k.id as kontroll_id, k.tittel as kontroll_tittel, 
                        t.id as testgrunnlag_id, t.namn as testgrunnlag_namn, 
                        t.dato_oppretta as testgrunnlag_dato_oppretta,
                        cast(coalesce(t.uuid,m.uuid) as text) as testrun_uuid,
                        "type" as testtype,
                        coalesce(t.id,m.id) as testgrunnlag_id,
                        coalesce(t.dato_oppretta, m.dato_start) as dato,
                        kontrolltype
                        from "testlab2_testing"."kontroll" k
                        left join "testlab2_testing"."testgrunnlag" t on t.kontroll_id=k.id
                        left join "testlab2_testing"."maalingv1" m on m.kontrollid=k.id
                        left join "testlab2_testing"."testgrunnlag_loeysing_nettside" tln on tln.testgrunnlag_id=t.id
                        left join "testlab2_testing"."maalingloeysing" ml  on ml.idmaaling=m.id
                        where k.id = :kontroll_id
                        """

        if (loeysingId != null)
            query = "$query and (ml.idloeysing = :loeysing_id or tln.loeysing_id = :loeysing_id)"


        return jdbcTemplate.query(query, mapOf("kontroll_id" to kontrollId, "loeysing_id" to loeysingId)) { rs, _ ->
            ResultatMetadata(
                kontrollId = rs.getInt("kontroll_id"),
                kontrollNamn = rs.getString("kontroll_tittel"),
                testrunUuid = rs.getString("testrun_uuid")?:"",
                testgrunnlagId = rs.getInt("testgrunnlag_id"),
                kontrollType = Kontrolltype.valueOf(rs.getString("kontrolltype")),
                testgrunnlagType = TestgrunnlagType.valueOf(rs.getString("testtype") ?: "OPPRINNELEG_TEST"),
                datoOppretta = handleDate(rs.getDate("dato")),
                testar = emptyList()
            )
        }
    }


    fun getTestresultatTestgrunnlag(): List<ResultatPerTestregelDTO> {
    return jdbcTemplate.query(queryTestresultatTestgrunnlag) { rs, _ ->
      resultatLoeysingRowmapper(rs)
    }
  }

  fun getTestresultatTestgrunnlag(testgrunnlagId: Int): List<ResultatPerTestregelDTO> {
    val query = "$queryTestresultatTestgrunnlag where testgrunnlag_id = :testgrunnlagId"
    return runCatching {
          jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
            resultatLoeysingRowmapper(rs)
          }
        }
        .getOrElse {
          logger.error(it.stackTraceToString())
          throw it
        }
  }

  @Observed(name = "getAllResultat", contextualName = "ResultatDAO.getAllResultat")
  fun getAllResultat(): List<ResultatPerTestregelDTO> {
    return jdbcTemplate.query(resultatQuery) { rs, _ -> resultatLoeysingRowmapper(rs) }
  }

}