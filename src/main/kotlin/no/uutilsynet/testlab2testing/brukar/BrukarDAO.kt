package no.uutilsynet.testlab2testing.brukar

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class BrukarDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {
  /**
   * Lagrar brukar i databasen. Om brukaren finst frå før, oppdaterar me namnet.
   *
   * @return id til brukaren
   */
  fun saveBrukar(brukar: Brukar): Int {
    return jdbcTemplate.queryForObject(
        """
            insert into brukar (brukarnamn, namn)
            values (:brukarnamn, :namn)
            on conflict (brukarnamn) do update
            set namn = :namn
            returning id
        """
            .trimIndent(),
        mapOf("brukarnamn" to brukar.brukarnamn, "namn" to brukar.namn),
        Int::class.java)!!
  }

  fun getBrukar(brukarnamn: String): Brukar? {
    return jdbcTemplate
        .query(
            """
                select brukarnamn, namn
                from brukar
                where brukarnamn = :brukarnamn
            """
                .trimIndent(),
            mapOf("brukarnamn" to brukarnamn)) { rs, _ ->
              Brukar(rs.getString("brukarnamn"), rs.getString("namn"))
            }
        .firstOrNull()
  }

  fun getBrukarId(brukarnamn: String): Int? {
    return jdbcTemplate
        .query(
            """
                    select id
                    from brukar
                    where brukarnamn = :brukarnamn
                """
                .trimIndent(),
            mapOf("brukarnamn" to brukarnamn)) { rs, _ ->
              rs.getInt("id")
            }
        .firstOrNull()
  }

  fun getBrukarById(brukarId: Int): Brukar? {
    return jdbcTemplate
        .query(
            """
                    select brukarnamn, namn
                    from brukar
                    where id = :brukarId
                """
                .trimIndent(),
            mapOf("brukarId" to brukarId)) { rs, _ ->
              Brukar(rs.getString("brukarnamn"), rs.getString("namn"))
            }
        .firstOrNull()
  }
}
