package no.uutilsynet.testlab2testing.inngaendekontroll.sak

import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import javax.sql.DataSource
import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.Testregel.Companion.toTestregelBase
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SakDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val dataSource: DataSource,
    val testregelDAO: TestregelDAO,
    val brukarDAO: BrukarDAO,
    val sideutvalDAO: SideutvalDAO
) {

  val logger = LoggerFactory.getLogger(SakDAO::class.java)

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
    val testreglar = testregelDAO.getTestreglarBySak(sakId)
    val rowMapper = RowMapper { rs, _ ->
      val loeysingIdArray = rs.getArray("loeysingar")?.array as? Array<Int> ?: emptyArray()
      val loeysingar =
          loeysingIdArray.map { loeysingId ->
            val nettsider = sideutvalDAO.findNettsiderBySakAndLoeysing(sakId, loeysingId)
            Sak.Loeysing(loeysingId, nettsider)
          }
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

  @Transactional
  fun getSakDTO(sakId: Int): Result<SakDTO> {
    val sak = getSak(sakId).getOrThrow()
    return Result.success(
        SakDTO(
            sak.id,
            sak.namn,
            sak.virksomhet,
            sak.frist,
            sak.ansvarleg,
            sak.loeysingar,
            sak.testreglar.map { it.toTestregelBase() }))
  }

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
    sideutvalDAO.deleteNettsiderForSak(sak.id)
    sak.loeysingar.forEach { loeysing ->
      sideutvalDAO.insertNettsiderForSak(sak.id, loeysing.loeysingId, loeysing.nettsider)
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

  @Transactional
  fun updateSakDTO(sakDTO: SakDTO): Result<SakDTO> {
    val sak = getSak(sakDTO.id).getOrThrow()
    val testreglar = getTestreglar(sakDTO.testreglar.map { it.id })
    val updatedSak =
        sak.copy(
            virksomhet = sakDTO.virksomhet,
            ansvarleg = sakDTO.ansvarleg,
            loeysingar = sakDTO.loeysingar,
            testreglar = testreglar)
    val updateStatus = update(updatedSak)
    updateStatus.onFailure {
      logger.error(it.message, it)
      return Result.failure(it)
    }

    return Result.success(
        SakDTO(
            updatedSak.id,
            updatedSak.namn,
            updatedSak.virksomhet,
            updatedSak.frist,
            updatedSak.ansvarleg,
            updatedSak.loeysingar,
            updatedSak.testreglar.map { it.toTestregelBase() }))
  }

  fun getTestreglar(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getTestregelList().filter { testregelIdList.contains(it.id) }
  }
}
