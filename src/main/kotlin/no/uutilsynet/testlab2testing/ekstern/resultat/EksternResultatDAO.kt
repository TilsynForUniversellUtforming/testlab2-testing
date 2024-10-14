package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class EksternResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun getTestsForLoeysingIds(loeysingIdList: List<Int>): List<TestListElementDB> {
    return jdbcTemplate.query(
        """
      select r.id_ekstern, tg.kontroll_id, r.loeysing_id, k.kontrolltype, r.publisert
      from kontroll k 
          join testgrunnlag tg on tg.kontroll_id = k.id
          join rapport r on tg.id = r.testgrunnlag_id
      where r.publisert is not null
          and tg.type = 'OPPRINNELEG_TEST'
          and r.loeysing_id in (:loeysingIdList)
    """
            .trimIndent(),
        mapOf("loeysingIdList" to loeysingIdList)) { rs, _ ->
          TestListElementDB(
              rs.getString("id_ekstern"),
              rs.getInt("kontroll_id"),
              rs.getInt("loeysing_id"),
              Kontrolltype.valueOf(rs.getString("kontrolltype")),
              rs.getTimestamp("publisert").toInstant())
        }
  }

  data class TestgrunnlagIdLoeysingId(
      val testgrunnlagId: Int,
      val loeysingId: Int,
  )

  fun findTestgrunnlagLoeysingFromRapportId(rapportId: String): TestgrunnlagIdLoeysingId? =
      jdbcTemplate.queryForObject(
          """
            select r.testgrunnlag_id, r.loeysing_id from rapport r where r.id_ekstern = :rapportId
      """
              .trimIndent(),
          mapOf("rapportId" to rapportId),
          DataClassRowMapper.newInstance(TestgrunnlagIdLoeysingId::class.java))


}
