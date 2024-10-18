package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EksternResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  val logger = LoggerFactory.getLogger(EksternResultatDAO::class.java)

  fun getTestsForLoeysingIds(loeysingIdList: List<Int>): List<TestListElementDB> {
    return jdbcTemplate.query(
        """
      select r.id_ekstern, tg.kontroll_id, r.loeysing_id, k.kontrolltype, r.publisert
      from kontroll k 
          join testgrunnlag tg on tg.kontroll_id = k.id
          join rapport r on k.id = r.kontroll_id
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

  data class KontrollIdLoeysingId(
      val kontrollId: Int,
      val loeysingId: Int,
  )

  fun findTestgrunnlagLoeysingFromRapportId(rapportId: String): KontrollIdLoeysingId? =
      jdbcTemplate.queryForObject(
          """
            select r.kontroll_id, r.loeysing_id from rapport r where r.id_ekstern = :rapportId
      """
              .trimIndent(),
          mapOf("rapportId" to rapportId),
          DataClassRowMapper.newInstance(KontrollIdLoeysingId::class.java))

  @Transactional
  fun publiserResultat(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
          insert into rapport(kontroll_id,loeysing_id,publisert) values(:kontrollId, :loeysingId , now())
        """
                  .trimIndent(),
              mapOf("kontrollId" to kontrollId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info("Publiserte resultat for testgrunnlag $kontrollId og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }
}
