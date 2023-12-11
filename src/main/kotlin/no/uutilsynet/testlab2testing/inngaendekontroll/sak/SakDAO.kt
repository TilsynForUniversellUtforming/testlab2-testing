package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SakDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val dataSource: DataSource,
    val testregelDAO: TestregelDAO
) {
  fun save(virksomhet: String): Result<Int> = runCatching {
    jdbcTemplate.queryForObject(
        "insert into sak (virksomhet, opprettet) values (:virksomhet, :opprettet) returning id",
        mapOf("virksomhet" to virksomhet, "opprettet" to Timestamp.from(Instant.now())),
        Int::class.java)!!
  }

  fun getSak(sakId: Int): Result<Sak> {
    val rowMapper = RowMapper { rs, _ ->
      val loeysingIdArray = rs.getArray("loeysingar")?.array as? Array<Int> ?: emptyArray()
      val loeysingar =
          loeysingIdArray.map { loeysingId ->
            val nettsider = findNettsiderBySakAndLoeysing(sakId, loeysingId)
            Sak.Loeysing(loeysingId, nettsider)
          }
      val testreglar = testregelDAO.getTestreglarBySak(sakId)
      Sak(sakId, rs.getString("virksomhet"), loeysingar, testreglar)
    }
    val sak =
        jdbcTemplate.queryForObject(
            """
                select virksomhet, loeysingar
                from sak
                where id = :id
            """
                .trimIndent(),
            mapOf("id" to sakId),
            rowMapper)
    return if (sak != null) Result.success(sak) else Result.failure(IllegalArgumentException())
  }

  private fun findNettsiderBySakAndLoeysing(
      sakId: Int,
      loeysingId: Int
  ): MutableList<Sak.Nettside> =
      jdbcTemplate.query(
          """
                    select id, type, url, beskrivelse, begrunnelse
                    from nettside
                    where id in (
                        select nettside_id
                        from sak_loeysing_nettside
                        where sak_id = :sak_id
                            and loeysing_id = :loeysing_id
                    )
                """
              .trimIndent(),
          mapOf("sak_id" to sakId, "loeysing_id" to loeysingId),
          DataClassRowMapper.newInstance(Sak.Nettside::class.java))

  @Transactional
  fun update(sak: Sak): Result<Sak> = runCatching {
    // oppdater lista med virksomheter
    val array =
        dataSource.connection.createArrayOf(
            "INTEGER", sak.loeysingar.map { it.loeysingId }.toTypedArray())
    jdbcTemplate.update(
        "update sak set virksomhet = :virksomhet, loeysingar = :loeysingar where id = :id",
        mapOf("virksomhet" to sak.virksomhet, "loeysingar" to array, "id" to sak.id))

    // slett alle nettsider som er knyttet til saken
    jdbcTemplate.update(
        """
              delete from sak_loeysing_nettside
              where sak_id = :sak_id
          """
            .trimIndent(),
        mapOf("sak_id" to sak.id))
    jdbcTemplate.update(
        """
        delete from nettside
        where id in (
            select nettside_id
            from sak_loeysing_nettside
            where sak_id = :sak_id
        )
    """
            .trimIndent(),
        mapOf("sak_id" to sak.id))

    // legg til alle nye nettsider som er knyttet til saken
    sak.loeysingar.forEach { loeysing ->
      loeysing.nettsider.forEach { nettside ->
        val nettsideId =
            jdbcTemplate.queryForObject(
                """
                insert into nettside (type, url, beskrivelse, begrunnelse)
                values (:type, :url, :beskrivelse, :begrunnelse)
                returning id
            """
                    .trimIndent(),
                mapOf(
                    "type" to nettside.type,
                    "url" to nettside.url,
                    "beskrivelse" to nettside.beskrivelse,
                    "begrunnelse" to nettside.begrunnelse),
                Int::class.java)!!
        jdbcTemplate.update(
            """
                  insert into sak_loeysing_nettside (sak_id, loeysing_id, nettside_id)
                  values (:sak_id, :loeysing_id, :nettside_id)
              """
                .trimIndent(),
            mapOf(
                "sak_id" to sak.id,
                "loeysing_id" to loeysing.loeysingId,
                "nettside_id" to nettsideId))
      }
    }

    // slett alle testreglar som er knyttet til saken
    jdbcTemplate.update(
        """
            delete from sak_testregel
            where sak_id = :sak_id
        """
            .trimIndent(),
        mapOf("sak_id" to sak.id))

    // legg til alle nye testreglar som er knyttet til saken
    sak.testreglar.forEach { testregel ->
      jdbcTemplate.update(
          """
              insert into sak_testregel (sak_id, testregel_id)
              values (:sak_id, :testregel_id)
          """
              .trimIndent(),
          mapOf("sak_id" to sak.id, "testregel_id" to testregel.id))
    }
    sak
  }
}
