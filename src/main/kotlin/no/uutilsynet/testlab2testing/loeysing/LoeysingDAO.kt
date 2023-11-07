package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LoeysingDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val loeysingsRegisterClient: LoeysingsRegisterClient
) {

  val createLoeysingSql =
      "insert into loeysing (namn, url, orgnummer) values (:namn, :url, :orgnummer) returning id"

  fun createLoeysingParams(namn: String, url: URL, orgnummer: String?) =
      mapOf("namn" to namn, "url" to url.toString(), "orgnummer" to orgnummer)

  val updateLoeysingSql =
      "update loeysing set namn = :namn, url = :url, orgnummer = :orgnummer where id = :id"

  fun updateLoeysingParams(loeysing: Loeysing) =
      mapOf(
          "namn" to loeysing.namn,
          "url" to loeysing.url.toString(),
          "orgnummer" to loeysing.orgnummer,
          "id" to loeysing.id)

  val deleteLoeysingSql = "delete from loeysing where id = :id"

  fun deleteLoeysingParams(id: Int) = mapOf("id" to id)

  val loeysingRowMapper = DataClassRowMapper.newInstance(Loeysing::class.java)

  /**
   * Bruk createLoeysing for produksjon. Denne er bare til test.
   *
   * I en mellomfase, mens vi går over til et nytt løysingsregister, lagrer vi løsninger både i
   * denne databasen, og i det nye registeret. Denne funksjonen er bare for test, og kommer til å
   * bli fjernet.
   */
  @Transactional
  fun createLoeysingInternal(namn: String, url: URL, orgnummer: String?): Int =
      jdbcTemplate.queryForObject(
          createLoeysingSql, createLoeysingParams(namn, url, orgnummer), Int::class.java)!!

  @Transactional
  fun createLoeysing(namn: String, url: URL, orgnummer: String): Int {
    val id =
        jdbcTemplate.queryForObject(
            createLoeysingSql, createLoeysingParams(namn, url, orgnummer), Int::class.java)!!
    loeysingsRegisterClient.saveLoeysing(id, namn, url, orgnummer)
    return id
  }

  @Transactional
  fun deleteLoeysing(id: Int) = jdbcTemplate.update(deleteLoeysingSql, deleteLoeysingParams(id))

  @Transactional
  fun updateLoeysing(loeysing: Loeysing) =
      jdbcTemplate.update(updateLoeysingSql, updateLoeysingParams(loeysing))

  fun getMaalingLoeysingListById(idloeysing: Int): List<Int> =
      jdbcTemplate.queryForList(
          "select idmaaling from maalingLoeysing where idloeysing in (:idloeysing)",
          mapOf("idloeysing" to idloeysing),
          Int::class.java)

  fun findLoeysingByURLAndOrgnummer(url: URL, orgnummer: String): Loeysing? {
    val sammeOrgnummer =
        jdbcTemplate.query(
            """
                select id, namn, url, orgnummer
                from loeysing
                where orgnummer = :orgnummer
            """
                .trimIndent(),
            mapOf("orgnummer" to orgnummer),
            loeysingRowMapper)
    return sammeOrgnummer.find { loeysing -> sameURL(loeysing.url, url) }
  }

  fun findLoeysingListForMaaling(maaling: Int): List<Loeysing> {
    return jdbcTemplate.query(
        """
            select id, namn, url, orgnummer
            from maalingloeysing ml
            join loeysing l
            on ml.idloeysing = l.id
            where ml.idmaaling = :maaling
        """
            .trimIndent(),
        mapOf("maaling" to maaling),
        loeysingRowMapper)
  }
}
