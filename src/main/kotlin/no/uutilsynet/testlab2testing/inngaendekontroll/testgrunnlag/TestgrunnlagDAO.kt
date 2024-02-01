package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TestgrunnlagDAO(
    val jdbcTemplate: NamedParameterJdbcTemplate
) {

    fun getTestgrunnlag(id: Int): Result<Testgrunnlag> {
        val result = jdbcTemplate.queryForObject(
            "select * from testgrunnlag where id = :id",
            mapOf("id" to id),
            Testgrunnlag::class.java
        )

        return if (result != null)
            return Result.success(result) else Result.failure(IllegalArgumentException())
    }

    fun getTestgrunnlagForSak(sakId: Int, loeysingId: Int?): List<Testgrunnlag> {
        return jdbcTemplate.queryForList(
            """select t.id, t.sak_id,testgruppering_id,namn,type,dato_oppretta 
                |from testgrunnlag t left join testgrunnlag_loeysing_nettside tln on t.id = tln.testgrunnlag_id 
                |where sak_id = :sakId
                | and ${if (loeysingId != null) "loeysing_id = :loeysingId" else "true"}
                 """.trimMargin(),
            mapOf("sakId" to sakId, "loeysingId" to loeysingId),
            Testgrunnlag::class.java
        )
    }

    fun createTestgrunnlag(Testgrunnlag: Testgrunnlag): Int {
        return jdbcTemplate.queryForObject(
            """insert into testgrunnlag (sak_id, testgruppering_id, namn, type, dato_oppretta)
                |values (:sakId, :testgrupperingId, :namn, :type, :datoOppretta)
                |returning id
            """.trimMargin(),
            mapOf(
                "sakId" to Testgrunnlag.sakId,
                "testgrupperingId" to Testgrunnlag.testgrupperingId,
                "namn" to Testgrunnlag.namn,
                "type" to Testgrunnlag.type,
                "datoOppretta" to Instant.now()
            ),
            Int::class.java
        )!!
    }

    fun updateTestgrunnlag(Testgrunnlag: Testgrunnlag) {
        jdbcTemplate.update(
            """update testgrunnlag set sak_id = :sakId, testgruppering_id = :testgrupperingId, namn = :namn, type = :type
                |where id = :id
            """.trimMargin(),
            mapOf(
                "id" to Testgrunnlag.id,
                "sakId" to Testgrunnlag.sakId,
                "testgrupperingId" to Testgrunnlag.testgrupperingId,
                "namn" to Testgrunnlag.namn,
                "type" to Testgrunnlag.type
            )
        )
    }

    fun deleteTestgrunnlag(id: Int) {
        jdbcTemplate.update(
            "delete from testgrunnlag where id = :id",
            mapOf("id" to id)
        )
    }
}