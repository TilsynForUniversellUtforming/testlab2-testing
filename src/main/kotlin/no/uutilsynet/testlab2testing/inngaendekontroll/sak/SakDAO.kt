package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDTO
import org.simpleflatmapper.jdbc.spring.JdbcTemplateMapperFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SakDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val dataSource: DataSource,
    val testregelDAO: TestregelDAO,
    val brukarDAO: BrukarDAO
) {
  fun save(namn: String, virksomhet: String, frist: LocalDate): Result<Int> = runCatching {
    jdbcTemplate.queryForObject(
        "insert into sak (namn, virksomhet, frist, opprettet) values (:namn, :virksomhet, :frist, :opprettet) returning id",
        mapOf(
            "namn" to namn,
            "virksomhet" to virksomhet,
            "frist" to Date.valueOf(frist),
            "opprettet" to Timestamp.from(Instant.now())),
        Int::class.java)!!
  }

  @Transactional
  fun getSak(sakId: Int): Result<Sak> {
    val rowMapper = RowMapper { rs, _ ->
      val loeysingIdArray = rs.getArray("loeysingar")?.array as? Array<Int> ?: emptyArray()
      val loeysingar =
          loeysingIdArray.map { loeysingId ->
            val nettsider = findNettsiderBySakAndLoeysing(sakId, loeysingId)
            Sak.Loeysing(loeysingId, nettsider)
          }
      val testreglar = testregelDAO.getTestreglarBySak(sakId)
      val brukarId = rs.getInt("ansvarleg_id")
      val ansvarleg =
          if (brukarId > 0)
              Brukar(rs.getString("ansvarleg_brukarnamn"), rs.getString("ansvarleg_namn"))
          else null
      Sak(
          sakId,
          rs.getString("namn"),
          rs.getString("virksomhet"),
          rs.getDate("frist").toLocalDate(),
          ansvarleg = ansvarleg,
          loeysingar = loeysingar,
          testreglar = testreglar)
    }
    val sak =
        jdbcTemplate.queryForObject(
            """
                select sak.namn, sak.virksomhet, sak.frist, sak.loeysingar, brukar.id as ansvarleg_id, brukar.namn as ansvarleg_namn, brukar.brukarnamn as ansvarleg_brukarnamn
                from sak
                left join brukar on brukar.id = sak.ansvarleg
                where sak.id = :id
            """
                .trimIndent(),
            mapOf("id" to sakId),
            rowMapper)
    return if (sak != null) Result.success(sak) else Result.failure(IllegalArgumentException())
  }

  fun getSakDTO(sakId: Int): Result<SakDTO> {
    val sak = getSak(sakId).getOrThrow()
    return Result.success(
        SakDTO(
            sak.id,
            sak.namn,
            sak.virksomhet,
            sak.loeysingar,
            sak.testreglar.map { TestregelDTO(it) }))
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
    updateAnsvarleg(sak)
    updateVirksomheter(sak)
    updateNettsider(sak)
    updateTestreglar(sak)
    sak
  }

  private fun updateAnsvarleg(sak: Sak) {
    val brukarId: Int? = sak.ansvarleg?.let { brukarDAO.saveBrukar(it) }
    jdbcTemplate.update(
        "update sak set ansvarleg = :brukarId where id = :id",
        mapOf("brukarId" to brukarId, "id" to sak.id))
  }

  private fun updateVirksomheter(sak: Sak) {
    val array =
        dataSource.connection.createArrayOf(
            "INTEGER", sak.loeysingar.map { it.loeysingId }.toTypedArray())
    jdbcTemplate.update(
        "update sak set virksomhet = :virksomhet, loeysingar = :loeysingar where id = :id",
        mapOf("virksomhet" to sak.virksomhet, "loeysingar" to array, "id" to sak.id))
  }

  /** Sletter alle koblinger mellom denne saken og testregler, og lagrer de nye koblingene. */
  private fun updateTestreglar(sak: Sak) {
    jdbcTemplate.update(
        """
            delete from sak_testregel
            where sak_id = :sak_id
        """
            .trimIndent(),
        mapOf("sak_id" to sak.id))

    sak.testreglar.forEach { testregel ->
      jdbcTemplate.update(
          """
              insert into sak_testregel (sak_id, testregel_id)
              values (:sak_id, :testregel_id)
          """
              .trimIndent(),
          mapOf("sak_id" to sak.id, "testregel_id" to testregel.id))
    }
  }

  /** Sletter alle nettsider som er lagret for denne saken, og lagrer de nye nettsidene. */
  private fun updateNettsider(sak: Sak) {
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
            where id not in (
                select nettside_id
                from sak_loeysing_nettside
            )
        """
            .trimIndent(),
        mapOf("sak_id" to sak.id))

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
  }

  fun getAlleSaker(): List<SakListeElement> {
    return jdbcTemplate.query(
        """
               select sak.id,
               sak.namn,
               sak.virksomhet,
               sak.frist,
               brukar.brukarnamn as ansvarleg_brukarnamn,
               brukar.namn as ansvarleg_namn
               from sak
                 left join brukar on sak.ansvarleg = brukar.id
               order by id
         """,
        emptyMap<String, String>()) { rs, rowNum ->
          val brukarnamn = rs.getString("ansvarleg_brukarnamn")
          val namn = rs.getString("ansvarleg_namn")
          val brukar =
              if (brukarnamn != null) {
                Brukar(brukarnamn, namn)
              } else null
          SakListeElement(
              rs.getInt("id"),
              rs.getString("namn"),
              rs.getString("virksomhet"),
              rs.getDate("frist").toLocalDate(),
              brukar)
        }
  }

  fun updateSakDTO(sakDTO: SakDTO): Result<SakDTO> {
    val sak = getSak(sakDTO.id).getOrThrow()
    val testreglar = getTestreglar(sakDTO.testreglar.map { it.id })
    val updatedSak =
        sak.copy(
            virksomhet = sakDTO.virksomhet, loeysingar = sakDTO.loeysingar, testreglar = testreglar)
    update(updatedSak)
    return Result.success(
        SakDTO(
            updatedSak.id,
            updatedSak.namn,
            updatedSak.virksomhet,
            updatedSak.loeysingar,
            updatedSak.testreglar.map { TestregelDTO(it) }))
  }

  fun getTestreglar(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getTestregelList().filter { testregelIdList.contains(it.id) }
  }
}
