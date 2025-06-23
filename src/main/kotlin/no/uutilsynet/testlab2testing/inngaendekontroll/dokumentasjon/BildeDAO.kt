package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.sql.Timestamp
import java.time.Instant
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.BildeSti
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BildeDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

  @Transactional
  fun saveBilde(testresultatId: Int, bildePath: String, thumbnailPath: String) = runCatching {
    jdbcTemplate.update(
        """
              insert into testresultat_bilde (testresultat_id, bilde, thumbnail, opprettet)
              values (:testresultat_id, :bilde, :thumbnail, :opprettet)
              on conflict (testresultat_id, bilde, thumbnail) do update
              set opprettet = excluded.opprettet;
            """
            .trimMargin(),
        mapOf(
            "testresultat_id" to testresultatId,
            "bilde" to bildePath,
            "thumbnail" to thumbnailPath,
            "opprettet" to Timestamp.from(Instant.now())))
  }

  @Transactional
  fun deleteBilde(bildeId: Int) = runCatching {
    jdbcTemplate.update(
        "delete from testresultat_bilde where id = :bilde_id", mapOf("bilde_id" to bildeId))
  }

  fun getBildeSti(bildeId: Int) = runCatching {
    DataAccessUtils.singleResult(
        jdbcTemplate.query(
            "select id, bilde, thumbnail, opprettet from testresultat_bilde where id = :id",
            mapOf("id" to bildeId),
            DataClassRowMapper.newInstance(BildeSti::class.java)))
  }

  fun getBildePathsForTestresultat(testresultatId: Int) = runCatching {
    jdbcTemplate
        .query(
            "select id, bilde, thumbnail, opprettet from testresultat_bilde where testresultat_id = :testresultat_id",
            mapOf("testresultat_id" to testresultatId),
            DataClassRowMapper.newInstance(BildeSti::class.java))
        .toList()
  }

  fun erBildeTilPublisertTestgrunnlag(bildesti: String): Boolean {
    val bildeQuery =
        """select exists(select tg.id, bilde, r.id_ekstern from testlab2_testing.testresultat_bilde tb
                    join testlab2_testing.testresultat tr on tb.testresultat_id=tr.id
                    join testlab2_testing.testgrunnlag tg on tr.testgrunnlag_id=tg.id
                    join testlab2_testing.rapport r on tg.id=r.testgrunnlag_id
                    where bilde=:bilde
                    and publisert is not null)"""

    val thumbnailQuery =
        """select exists(select tg.id, thumbnail, r.id_ekstern from testlab2_testing.testresultat_bilde tb
                    join testlab2_testing.testresultat tr on tb.testresultat_id=tr.id
                    join testlab2_testing.testgrunnlag tg on tr.testgrunnlag_id=tg.id
                    join testlab2_testing.rapport r on tg.id=r.testgrunnlag_id
                    where thumbnail=:bilde
                    and publisert is not null)"""

    val query = setCorrectQuery(bildesti, thumbnailQuery, bildeQuery)

    return jdbcTemplate.queryForObject(query, mapOf("bilde" to bildesti), Boolean::class.java)
        ?: false
  }

  private fun setCorrectQuery(
      bildesti: String,
      thumbnailQuery: String,
      bildeQuery: String
  ): String {
    val query =
        if (bildesti.contains("thumb")) {
          thumbnailQuery
        } else {
          bildeQuery
        }
    return query
  }
}
