package no.uutilsynet.testlab2testing.resultat

import java.sql.ResultSet
import java.time.LocalDate
import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagType
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  private val logger = LoggerFactory.getLogger(ResultatDAO::class.java)

  val resultatQuery =
      """
        select 
          k.id     as id,
          k.tittel as tittel,
          testgrunnlag_id,
          testtype,
          kontrolltype,
          loeysing_id,
          testregel_gjennomsnittleg_side_samsvar_prosent,
          tal_element_samsvar,
          tal_element_brot,
          kontroll_id,
          dato,
          testregel_id
        from kontroll k
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
              left join maalingv1 m on m.id = agt.maaling_id
              left join testgrunnlag t on t.id = agt.testgrunnlag_id
            where case
              when maaling_id is not null
                then m.kontrollid
                else t.kontroll_id
              end is not null
            ) as ag
        on k.id = ag.kontroll_id
      """

  fun getTestresultatMaaling(): List<ResultatLoeysing> {
    val query =
        """
        select k.id, k.tittel as tittel,kontrolltype, maaling_id as testgrunnlag_id, 'OPPRINNELIG_TEST' as testtype,
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
    val kontrolltype = Kontrolltype.valueOf(rs.getString("kontrolltype"))
    val loeysingId = rs.getInt("loeysing_id")
    val testregelGjennomsnittlegSideSamsvarProsent =
        rs.getDouble("testregel_gjennomsnittleg_side_samsvar_prosent")
    val talElementSamsvar = rs.getInt("tal_element_samsvar")
    val talElementBrot = rs.getInt("tal_element_brot")
    val testregelId = rs.getInt("testregel_id")
    val testgrunnlagId = rs.getInt("testgrunnlag_id")
    val testtype = setTestType(kontrolltype, rs)

    return ResultatLoeysing(
        maalingId,
        testgrunnlagId,
        navn,
        kontrolltype,
        TestgrunnlagType.valueOf(testtype),
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

  fun setTestType(kontrolltype: Kontrolltype, resultSet: ResultSet): String {
    if (kontrolltype == Kontrolltype.ForenklaKontroll) {
      return TestgrunnlagType.OPPRINNELEG_TEST.toString()
    }
    return resultSet.getString("testtype")
  }

  fun getTestresultatTestgrunnlag(): List<ResultatLoeysing> {
    val query =
        """
               select k.id as id, k.tittel as tittel, kontrolltype, testgrunnlag_id, type as testtype, loeysing_id, testregel_gjennomsnittleg_side_samsvar_prosent, tal_element_samsvar,tal_element_brot,
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
    val query = "$resultatQuery where k.id = :kontrollId order by testgrunnlag_id"
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

  fun getResultatKontrollLoeysing(kontrollId: Int, loeysingId: Int): List<ResultatLoeysing> {
    return runCatching {
          val query = "$resultatQuery where k.id = :kontrollId  and loeysing_id = :loeysingId"
          jdbcTemplate.query(
              query, mapOf("kontrollId" to kontrollId, "loeysingId" to loeysingId)) { rs, _ ->
                resultatLoeysingRowmapper(rs)
              }
        }
        .getOrThrow()
  }

  fun getResultatTestgrunnlagLoeysing(
      testgrunnlagId: Int,
      loeysingId: Int
  ): List<ResultatLoeysing> {
    val query =
        "$resultatQuery where k.testgrunnlag_id = :testgrunnlagId and loeysing_id = :loeysingId"
    return jdbcTemplate.query(query, mapOf("testgrunnlagId" to testgrunnlagId)) { rs, _ ->
      resultatLoeysingRowmapper(rs)
    }
  }

  fun getResultatPrTema(
      kontrollId: Int?,
      kontrolltype: Kontrolltype?,
      startDato: LocalDate?,
      sluttDato: LocalDate?
  ): List<ResultatTema> {
    runCatching {
          val query =
              """
          select 
            tema, 
            sum(tal_element_samsvar)                             as tal_element_samsvar,
            sum(tal_element_brot)                                as tal_element_brot,
            sum(tal_element_varsel)                              as tal_element_varsel,
            sum(tal_element_ikkje_forekomst)                     as tal_element_ikkje_forekomst,
            avg(testregel_gjennomsnittleg_side_samsvar_prosent ) as score
          from kontroll k
            join (
              select 
                tm.tema,
                loeysing_id,
                testregel_gjennomsnittleg_side_samsvar_prosent,
                tal_element_samsvar,
                tal_element_brot,
                tal_element_varsel,
                tal_element_ikkje_forekomst,
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
                left join maalingv1 m on m.id = agt.maaling_id
                left join testgrunnlag t on t.id = agt.testgrunnlag_id 
                  and t.type = 'OPPRINNELEG_TEST'
                left join testregel tr on tr.id = agt.testregel_id
                left join tema tm on tm.id = tr.tema
              where case
                when maaling_id is not null
                then m.kontrollid
                else t.kontroll_id
              end is not null
            ) as ag
            on k.id = ag.kontroll_id
            where 
              (:kontrollId::int is null or k.id = :kontrollId::int) and
              (:kontrollType::varchar is null or k.kontrolltype = :kontrollType::varchar) and
              (:startDato::timestamptz is null or ag.dato >= :startDato::timestamptz) and
              (:sluttDato::timestamptz is null or ag.dato <= :sluttDato::timestamptz)
          group by tema
        """

          return jdbcTemplate.query(
              query,
              mapOf(
                  "kontrollId" to kontrollId,
                  "kontrollType" to kontrolltype?.name,
                  "startDato" to startDato,
                  "sluttDato" to sluttDato)) { rs, _ ->
                resultatTemaRowmapper(rs)
              }
        }
        .getOrElse {
          logger.error(it.message)
          throw it
        }
  }

  private fun resultatTemaRowmapper(rs: ResultSet) =
      ResultatTema(
          rs.getString("tema") ?: "Null",
          rs.getInt("score"),
          rs.getInt("tal_element_samsvar") +
              rs.getInt("tal_element_brot") +
              rs.getInt("tal_element_varsel") +
              rs.getInt("tal_element_ikkje_forekomst"),
          rs.getInt("tal_element_samsvar"),
          rs.getInt("tal_element_brot"),
          rs.getInt("tal_element_varsel"),
          rs.getInt("tal_element_ikkje_forekomst"))

  fun getResultatPrKrav(
      kontrollId: Int?,
      kontrollType: Kontrolltype?,
      fraDato: LocalDate?,
      tilDato: LocalDate?
  ): List<ResultatKravBase> {
    kotlin
        .runCatching {
          println(
              "kontrollId: $kontrollId kontrollType: $kontrollType fraDato: $fraDato tilDato: $tilDato")

          val query =
              """
          select krav_id,
            sum(tal_element_samsvar)                            as tal_element_samsvar,
            sum(tal_element_brot)                               as tal_element_brot,
            sum(tal_element_varsel)                             as tal_element_varsel,
            sum(tal_element_ikkje_forekomst)                    as tal_element_ikkje_forekomst,
            avg(testregel_gjennomsnittleg_side_samsvar_prosent) as score
          from kontroll k
            join (
            select tr.krav_id as krav_id,
              loeysing_id,
              testregel_gjennomsnittleg_side_samsvar_prosent,
              tal_element_samsvar,
              tal_element_brot,
              tal_element_varsel,
              tal_element_ikkje_forekomst,
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
              left join maalingv1 m on m.id = agt.maaling_id
              left join testgrunnlag t on t.id = agt.testgrunnlag_id 
                and t.type = 'OPPRINNELEG_TEST'
              left join testregel tr on tr.id = agt.testregel_id
            where 
              case
                when maaling_id is not null
                then m.kontrollid
                else t.kontroll_id
              end is not null
            ) as ag
            on k.id = ag.kontroll_id
          where 
              (:kontrollId::int is null or k.id = :kontrollId::int) and
              (:kontrollType::varchar is null or k.kontrolltype = :kontrollType::varchar) and
              (:startDato::timestamptz is null or ag.dato >= :startDato::timestamptz) and
              (:sluttDato::timestamptz is null or ag.dato <= :sluttDato::timestamptz)
          group by krav_id
        """

          val rowMapper = DataClassRowMapper.newInstance(ResultatKravBase::class.java)
          rowMapper.isPrimitivesDefaultedForNullValue = true

          return jdbcTemplate.query(
              query,
              mapOf(
                  "kontrollId" to kontrollId,
                  "kontrollType" to kontrollType?.name,
                  "startDato" to fraDato,
                  "sluttDato" to tilDato)) { rs, _ ->
                ResultatKravBase(
                    rs.getInt("krav_id"),
                    rs.getInt("score"),
                    rs.getInt("tal_element_samsvar") +
                        rs.getInt("tal_element_brot") +
                        rs.getInt("tal_element_varsel") +
                        rs.getInt("tal_element_ikkje_forekomst"))
              }
        }
        .getOrElse {
          logger.error(it.stackTraceToString())
          throw it
        }
  }
}
