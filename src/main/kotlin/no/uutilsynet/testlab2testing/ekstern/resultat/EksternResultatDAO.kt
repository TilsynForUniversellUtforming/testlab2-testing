package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class EksternResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  val logger = LoggerFactory.getLogger(EksternResultatDAO::class.java)

  fun getTestsForLoeysingIds(loeysingIdList: List<Int>): List<TestListElementDB> {
    return jdbcTemplate.query(
        """select r.id_ekstern, case when tg.kontroll_id is not null then tg.kontroll_id else m.kontrollid end as kontroll_id , r.loeysing_id, case when k1.kontrolltype is not null then k1.kontrolltype else k2.kontrolltype end as kontrolltype, r.publisert
            from testlab2_testing.rapport r
            left join testlab2_testing.testgrunnlag tg on r.testgrunnlag_id=tg.id
            left join testlab2_testing.maalingv1 m on r.maaling_id=m.id
            left join testlab2_testing.kontroll k1 on tg.kontroll_id=k1.id
            left join testlab2_testing.kontroll k2 on m.kontrollid=k2.id
            where publisert is not null
          and r.loeysing_id in (:loeysingIdList)"""
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

  fun findKontrollLoeysingFromRapportId(rapportId: String): Result<List<KontrollIdLoeysingId>> =
      runCatching {
        jdbcTemplate.query(
            """
            select case when r.maaling_id is not null then m.kontrollid else t.kontroll_id end as kontroll_id, r.loeysing_id
            from rapport r 
            left join maalingv1 m on m.id=r.maaling_id
            left join testgrunnlag t on t.id=r.testgrunnlag_id
            where r.id_ekstern=:rapportId
      """
                .trimIndent(),
            mapOf("rapportId" to rapportId),
            DataClassRowMapper.newInstance(KontrollIdLoeysingId::class.java))
      }

  fun publisertTestgrunnlagResultat(testgrunnlagId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
          insert into rapport(testgrunnlag_id,loeysing_id,publisert) values(:testgrunnlagId, :loeysingId,now()) on conflict(testgrunnlag_id,loeysing_id) do update set publisert = now()
        """
                  .trimIndent(),
              mapOf("testgrunnlagId" to testgrunnlagId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info("Publiserte resultat for testgrunnlag  og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun publiserMaalingResultat(maalingId: Int, loeysingId: Int): Result<Boolean> {

    runCatching {
          jdbcTemplate.update(
              """
          insert into rapport(maaling_id,loeysing_id,publisert) values(:maalingId, :loeysingId,now()) on conflict(maaling_id,loeysing_id) do update set publisert = now()
        """
                  .trimIndent(),
              mapOf("maalingId" to maalingId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info("Publiserte resultat for maaling $maalingId og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun avpubliserResultatTestgrunnlag(testgrunnlagId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
          update rapport set publisert = null where testgrunnlag_id = :testgrunnlagId and loeysing_id = :loeysingId
        """
                  .trimIndent(),
              mapOf("testgrunnlagId" to testgrunnlagId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info(
                  "Avpubliserte resultat for testgrunnlag $testgrunnlagId og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun avpubliserResultatMaaling(maalingId: Int, loeysingId: Int): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              """
          update rapport set publisert = null where maaling_id = :maalingId and loeysing_id = :loeysingId
        """
                  .trimIndent(),
              mapOf("maalingId" to maalingId, "loeysingId" to loeysingId))
        }
        .fold(
            onSuccess = {
              logger.info("Avpubliserte resultat for maaling $maalingId og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              return Result.failure(it)
            })
  }

  fun erKontrollPublisert(id: Int, typeKontroll: Kontrolltype): Boolean {
    val query = erPublisertQuery(typeKontroll)

    runCatching {
          jdbcTemplate.queryForObject(
              query.trimIndent(), mapOf("kontrollId" to id), Int::class.java) == 1
        }
        .fold(
            onSuccess = {
              return it
            },
            onFailure = {
              logger.error("Feil ved henting av publisert status for kontroll $id", it)
              return false
            })
  }

  fun erPublisertQuery(kontrolltype: Kontrolltype): String {
    return if (kontrolltype == Kontrolltype.Statusmaaling ||
        kontrolltype == Kontrolltype.ForenklaKontroll) {
      "select count(*) from rapport r join maalingv1 m on m.id=r.maaling_id where m.kontrollid=:kontrollId and publisert is not null"
    } else {
      "select count(*) from rapport r join testgrunnlag tg on tg.id=r.testgrunnlag_id where tg.kontroll_id=:kontrollId and publisert is not null"
    }
  }
}
