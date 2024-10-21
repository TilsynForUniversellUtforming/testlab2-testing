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
      select r.id_ekstern, k.kontroll_id, r.loeysing_id, k.kontrolltype, r.publisert
      from kontroll k 
          join rapport r on k.id = r.kontroll_id
      where r.publisert is not null
          and r.loeysing_id in (:loeysingIdList)
    """
            .trimIndent(),
        mapOf("loeysingIdList" to loeysingIdList)) { rs, _ ->
          TestListElementDB(
              rs.getString("id_ekstern"),
              rs.getInt("id"),
              rs.getInt("loeysing_id"),
              Kontrolltype.valueOf(rs.getString("kontrolltype")),
              rs.getTimestamp("publisert").toInstant())
        }
  }

  data class KontrollIdLoeysingId(
      val kontrollId: Int,
      val loeysingId: Int,
  )

  fun findKontrollLoeysingFromRapportId(rapportId: String): KontrollIdLoeysingId? =
      jdbcTemplate.queryForObject(
          """
            select r.kontroll_id, r.loeysing_id from rapport r where r.id_ekstern = :rapportId
      """
              .trimIndent(),
          mapOf("rapportId" to rapportId),
          DataClassRowMapper.newInstance(KontrollIdLoeysingId::class.java))

  fun publiserResultat(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    return publiserNyttResultat(kontrollId, loeysingId)
  }

  private fun publiserNyttResultat(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
          insert into rapport(kontroll_id,loeysing_id,publisert) values(:kontrollId, :loeysingId,now()) on conflict(kontroll_id,loeysing_id) do update set publisert = now()
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

  @Transactional
  fun avpubliserResultat(kontrollId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
                update rapport set publisert = null where kontroll_id = :kontrollId and loeysing_id = :loeysingId
                """
                  .trimIndent(),
              mapOf("kontrollId" to kontrollId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info(
                  "Avpubliserte resultat for testgrunnlag $kontrollId og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun erKontrollPublisert(kontrollId: Int): Boolean {
    runCatching {
          jdbcTemplate.queryForObject(
              """
                select count(*) from rapport where kontroll_id = :kontrollId and publisert is not null
                """
                  .trimIndent(),
              mapOf("kontrollId" to kontrollId),
              Int::class.java) == 1
        }
        .fold(
            onSuccess = {
              return it
            },
            onFailure = {
              logger.error("Feil ved henting av publisert status for kontroll $kontrollId", it)
              return false
            })
  }
}
