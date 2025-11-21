package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2.constants.Kontrolltype
import no.uutilsynet.testlab2testing.ekstern.resultat.model.KontrollIdLoeysingId
import no.uutilsynet.testlab2testing.ekstern.resultat.model.TestListElementDB
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class EksternResultatDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  val logger = LoggerFactory.getLogger(EksternResultatDAO::class.java)

  fun getTestsForLoeysingIds(loeysingIdList: List<Int>): List<TestListElementDB> {
    return jdbcTemplate.query(
        """select r.id_ekstern, coalesce(tg.kontroll_id, m.kontrollid) as kontroll_id , r.loeysing_id, 
            coalesce(k1.kontrolltype, k2.kontrolltype) as kontrolltype, 
            coalesce(k1.tittel, k2.tittel) as kontrollnamn,r.publisert,
            coalesce(tg.dato_oppretta,m.dato_start) as utfoert
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
              rs.getString("kontrollnamn"),
              rs.getTimestamp("publisert").toInstant(),
              rs.getTimestamp("utfoert").toInstant())
        }
  }

  fun getTestsForRapportIds(rapportId: String): List<TestListElementDB> {
    return jdbcTemplate.query(
        """select r.id_ekstern, coalesce(tg.kontroll_id, m.kontrollid) as kontroll_id , r.loeysing_id, 
            coalesce(k1.kontrolltype, k2.kontrolltype) as kontrolltype, 
            coalesce(k1.tittel, k2.tittel) as kontrollnamn,r.publisert,
            coalesce(tg.dato_oppretta,m.dato_start) as utfoert
            from testlab2_testing.rapport r
            left join testlab2_testing.testgrunnlag tg on r.testgrunnlag_id=tg.id
            left join testlab2_testing.maalingv1 m on r.maaling_id=m.id
            left join testlab2_testing.kontroll k1 on tg.kontroll_id=k1.id
            left join testlab2_testing.kontroll k2 on m.kontrollid=k2.id
            where publisert is not null
            and r.id_ekstern = :rapportId"""
            .trimIndent(),
        mapOf("rapportId" to rapportId)) { rs, _ ->
          TestListElementDB(
              rs.getString("id_ekstern"),
              rs.getInt("kontroll_id"),
              rs.getInt("loeysing_id"),
              Kontrolltype.valueOf(rs.getString("kontrolltype")),
              rs.getString("kontrollnamn"),
              rs.getTimestamp("publisert").toInstant(),
              rs.getTimestamp("utfoert").toInstant())
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

  fun publisertTestgrunnlagResultat(
      testgrunnlagId: Int,
      loeysingId: Int,
      rapportId: String?
  ): Result<Boolean> {
    runCatching {
          jdbcTemplate.update(
              publiseringsQueryTestgrunnlag(rapportId).trimIndent(),
              mapOf(
                  "testgrunnlagId" to testgrunnlagId,
                  "loeysingId" to loeysingId,
                  "rapportId" to rapportId))
        }
        .fold(
            onSuccess = {
              logger.info("Publiserte resultat for testgrunnlag  og løysing $loeysingId")
              return Result.success(true)
            },
            onFailure = {
              logger.error(
                  "Feila ved publisering av resultat for testgrunnlag $testgrunnlagId og løysing $loeysingId",
                  it)
              return Result.failure(it)
            })
  }

  fun publiserMaalingResultat(
      maalingId: Int,
      loeysingId: Int,
      rapportId: String?
  ): Result<Boolean> {

    runCatching {
          jdbcTemplate.update(
              publiseringsQueryMaaling(rapportId).trimIndent(),
              mapOf("maalingId" to maalingId, "loeysingId" to loeysingId, "rapportId" to rapportId))
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

  private fun publiseringsQueryMaaling(rapportId: String?): String {
    return if (rapportId != null) {
      """insert into rapport(id_ekstern,maaling_id,loeysing_id,publisert) values(:rapportId,:maalingId, :loeysingId,now()) on conflict(maaling_id,loeysing_id) do update set publisert = now(), id_ekstern = :rapportId"""
    } else {
      """insert into rapport(maaling_id,loeysing_id,publisert) values(:maalingId, :loeysingId,now()) on conflict(maaling_id,loeysing_id) do update set publisert = now(), id_ekstern = :rapportId"""
    }
  }

  private fun publiseringsQueryTestgrunnlag(rapportId: String?): String {
    return if (rapportId != null) {
      """
          insert into rapport(id_ekstern,testgrunnlag_id,loeysing_id,publisert) values(:rapportId,:testgrunnlagId, :loeysingId,now()) on conflict(testgrunnlag_id,loeysing_id) do update set publisert = now()
        """
    } else {
      """
          insert into rapport(testgrunnlag_id,loeysing_id,publisert) values(:testgrunnlagId, :loeysingId,now()) on conflict(testgrunnlag_id,loeysing_id) do update set publisert = now()
        """
    }
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

  fun erKontrollPublisert(kontrollId: Int, typeKontroll: Kontrolltype): Boolean {
    val query = erPublisertQuery(typeKontroll)

    runCatching {
          jdbcTemplate.queryForObject(
              query.trimIndent(), mapOf("id" to kontrollId), Int::class.java)
              ?: 0
        }
        .fold(
            onSuccess = {
              return it > 0
            },
            onFailure = {
              logger.error("Feil ved henting av publisert status for kontroll $kontrollId", it)
              return false
            })
  }

  fun erPublisertQuery(kontrolltype: Kontrolltype): String {
    return if (kontrolltype == Kontrolltype.Statusmaaling ||
        kontrolltype == Kontrolltype.ForenklaKontroll) {
      "select count(*) from rapport r join maalingv1 m on m.id=r.maaling_id where m.kontrollid=:id and publisert is not null"
    } else {
      "select count(*) from rapport r join testgrunnlag tg on tg.id=r.testgrunnlag_id where tg.kontroll_id=:id and publisert is not null"
    }
  }

  fun getRapportIdForKontroll(kontrollId: Int): Result<String?> {
    return runCatching {
      jdbcTemplate
          .queryForList(
              """
                select r.id_ekstern from rapport r 
                left join maalingv1 m on m.id=r.maaling_id
                left join testgrunnlag t on t.id=r.testgrunnlag_id
                where case when r.maaling_id is not null then m.kontrollid else t.kontroll_id end = :kontrollId
            """
                  .trimIndent(),
              mapOf("kontrollId" to kontrollId),
              String::class.java)
          .firstOrNull()
    }
  }
}
