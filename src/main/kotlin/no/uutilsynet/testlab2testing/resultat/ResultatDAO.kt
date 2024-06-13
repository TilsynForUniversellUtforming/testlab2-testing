package no.uutilsynet.testlab2testing.resultat

import java.sql.ResultSet
import java.time.LocalDate
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import no.uutilsynet.testlab2testing.kontroll.Kontroll
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  val resultatQuery =
      """select k.id as id, k.tittel as tittel, kontrolltype, loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot, kontroll_id,dato, testregel_id
        from kontroll k
        join (
        select loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot, testregel_id, maaling_id, testgrunnlag_id,
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
		from aggregering_testregel agt
		left join maalingv1 m on m.id=agt.maaling_id
		left join testgrunnlag t on t.id=agt.testgrunnlag_id
		where case 
			when maaling_id is not null
				then m.kontrollid
				else t.kontroll_id 
			end is not null
		) as ag 
        on k.id=ag.kontroll_id
            """

  fun getTestresultatMaaling(): List<ResultatLoeysing> {
    val query =
        """
        select k.id, k.tittel as tittel,kontrolltype,
        dato_start as dato,
        loeysing_id, testregel_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot
        from kontroll k
        left join maalingv1 m on m.kontrollid=k.id
        join aggregering_testregel agt on agt.maaling_id=m.id
        """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ -> resultatLoeysingRowmapper(rs) }
  }

  private fun resultatLoeysingRowmapper(rs: ResultSet): ResultatLoeysing {
    val maalingId = rs.getInt("id")
    val navn = rs.getString("tittel")
    val dato = handleDate(rs.getDate("dato"))
    val kontrolltype = Kontroll.Kontrolltype.valueOf(rs.getString("kontrolltype"))
    val loeysingId = rs.getInt("loeysing_id")
    val testregelGjennomsnittlegSideSamsvarProsent =
        rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
    val talElementSamsvar = rs.getInt("tal_element_samsvar")
    val talElementBrot = rs.getInt("tal_element_brot")
    val testregelId = rs.getInt("testregel_id")

    return ResultatLoeysing(
        maalingId,
        navn,
        kontrolltype,
        TestgrunnlagType.OPPRINNELEG_TEST,
        dato,
        listOf("testar"),
        loeysingId,
        testregelGjennomsnittlegSideSamsvarProsent,
        talElementSamsvar,
        talElementBrot,
        testregelId,
        null,
        null)
  }

  fun getTestresultatTestgrunnlag(): List<ResultatLoeysing> {
    val query =
        """
               select k.id as id, k.tittel as tittel, kontrolltype, loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot,
            dato_oppretta as dato
            from kontroll k
            left join testgrunnlag t on t.kontroll_id=k.id
            join aggregering_testregel agt on agt.testgrunnlag_id=t.id
        """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ -> resultatLoeysingRowmapper(rs) }
  }

  fun getResultat(): List<ResultatLoeysing> {
    return jdbcTemplate.query(resultatQuery) { rs, _ -> resultatLoeysingRowmapper(rs) }
  }

  fun getResultatKontroll(kontrollId: Int): List<ResultatLoeysing> {
    val query = "$resultatQuery where k.id = :kontrollId"
    return jdbcTemplate.query(query, mapOf("kontrollId" to kontrollId)) { rs, _ ->
      resultatLoeysingRowmapper(rs)
    }
  }

  fun handleDate(date: java.sql.Date?): LocalDate {
    if (date != null) {
      return date.toLocalDate()
    }
    return LocalDate.now()
  }

  fun getNamn(kontrolltype: Kontroll.Kontrolltype, testgrunnlagId: Int, namn: String): String {
    if (kontrolltype == Kontroll.Kontrolltype.ForenklaKontroll) {
      return namn
    }
    val query =
        """
            select sak.namn
            from testgrunnlag 
            left join sak on sak.id =sak_id
            where testgrunnlag.id = :testgrunnlagId
        """
            .trimIndent()

    // Ekstra sjekk for periode med migrering mellom fleire datamodellar
    return try {
      jdbcTemplate.queryForObject(
          query, mapOf("testgrunnlagId" to testgrunnlagId), String::class.java)
          ?: ""
    } catch (e: EmptyResultDataAccessException) {
      namn
    }
  }

  fun getResultatKontrollLoeysing(kontrollId: Int, loeysingId: Int): List<ResultatLoeysing>? {
    val query = "$resultatQuery where k.id = :kontrollId  and loeysing_id = :loeysingId"
    return jdbcTemplate.query(
        query, mapOf("kontrollId" to kontrollId, "loeysingId" to loeysingId)) { rs, _ ->
          resultatLoeysingRowmapper(rs)
        }
  }

  fun getResultatPrTema(): List<ResultatTema> {
    val query =
        """
            select ta.tema, sum(tal_element_samsvar) as tal_element_samsvar,sum(tal_element_brot) as tal_element_brot,sum(tal_element_varsel) as tal_element_varsel,sum(tal_element_ikkje_forekomst) as tal_element_ikkje_forekomst, avg(testregel_gjennomsnittleg_side_samsvar_prosent ) as score
            from testlab2_testing.aggregering_testregel at
            left join testlab2_testing.testregel t on t.id=at.testregel_id
            left join testlab2_testing.tema ta on t.tema=ta.id
			group by ta.tema
        """
            .trimIndent()

    return jdbcTemplate.query(query) { rs, _ -> resultatTemaRowmapper(rs) }
  }

  fun getResultatPrTema(kontrollId: Int): List<ResultatTema> {
    val query =
        """
            select tema, sum(tal_element_samsvar) as tal_element_samsvar,sum(tal_element_brot) as tal_element_brot,sum(tal_element_varsel) as tal_element_varsel,sum(tal_element_ikkje_forekomst) as tal_element_ikkje_forekomst, avg(testregel_gjennomsnittleg_side_samsvar_prosent ) as score
        from kontroll k
        join (
        select tm.tema,loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot,tal_element_varsel,tal_element_ikkje_forekomst,
		case 
			when maaling_id is not null
				then m.kontrollid
				else t.kontroll_id 
			end as kontroll_id
		from aggregering_testregel agt
		left join maalingv1 m on m.id=agt.maaling_id
		left join testgrunnlag t on t.id=agt.testgrunnlag_id
		left join testregel tr on tr.id=agt.testregel_id
		left join tema tm on tm.id=tr.tema
		where case 
			when maaling_id is not null
				then m.kontrollid
				else t.kontroll_id 
			end is not null
		) as ag 
        on k.id=ag.kontroll_id
		where k.id=58
		group by tema
        """

    return jdbcTemplate.query(query) { rs, _ -> resultatTemaRowmapper(rs) }
  }

  private fun resultatTemaRowmapper(rs: ResultSet) =
      ResultatTema(
          rs.getString("tema") ?: "Null",
          rs.getInt("score").toInt(),
          rs.getInt("tal_element_samsvar") +
              rs.getInt("tal_element_brot") +
              rs.getInt("tal_element_varsel") +
              rs.getInt("tal_element_ikkje_forekomst"),
          rs.getInt("tal_element_samsvar"),
          rs.getInt("tal_element_brot"),
          rs.getInt("tal_element_varsel"),
          rs.getInt("tal_element_ikkje_forekomst"))
}
