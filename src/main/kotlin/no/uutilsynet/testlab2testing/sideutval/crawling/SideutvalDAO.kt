package no.uutilsynet.testlab2testing.sideutval.crawling

import no.uutilsynet.testlab2testing.forenkletkontroll.Framgang
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.DataClassRowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.URL
import java.sql.ResultSet
import java.sql.Timestamp

@Component
class SideutvalDAO(val jdbcTemplate: NamedParameterJdbcTemplate) {

    val logger: Logger = LoggerFactory.getLogger(SideutvalDAO::class.java)

    @Transactional
    fun saveCrawlResultat(crawlResultat: CrawlResultat, maalingId: Int) {

        if (crawlResultat is CrawlResultat.Ferdig &&
            crawlresultatAlreadyFinished(crawlResultat, maalingId)
        )
            return

        jdbcTemplate.update(
            "delete from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id",
            mapOf("loeysingid" to crawlResultat.loeysing.id, "maaling_id" to maalingId)
        )
        when (crawlResultat) {
            is CrawlResultat.IkkjeStarta -> {
                saveCrawlresultatIkkjeStarta(crawlResultat, maalingId)
            }

            is CrawlResultat.Starta -> {
                saveCrawlresultatStarta(crawlResultat, maalingId)
            }

            is CrawlResultat.Feila -> {
                saveCrawlresultatFeila(crawlResultat, maalingId)
            }

            is CrawlResultat.Ferdig -> {
                saveCrawlresultatFerdig(maalingId, crawlResultat)
            }
        }
    }

    private fun saveCrawlresultatFerdig(maalingId: Int, crawlResultat: CrawlResultat.Ferdig) {
        logger.debug(
            "CrawlResultat.Ferdig insert start. maalingId: $maalingId, loeysingId: ${crawlResultat.loeysing.id}"
        )

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update(
            "insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)",
            MapSqlParameterSource(
                mapOf(
                    "loeysingid" to crawlResultat.loeysing.id,
                    "status" to status(crawlResultat),
                    "status_url" to crawlResultat.statusUrl.toString(),
                    "maaling_id" to maalingId,
                    "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert)
                )
            ),
            keyHolder,
            arrayOf("id")
        )

        val id = keyHolder.key?.toInt() ?: throw RuntimeException("Feil ved insert av CrawlResultat")

        logger.debug(
            "CrawlResultat.Ferdig insert ferdig. maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id} ny crid: $id"
        )

        crawlResultat.nettsider.forEach { nettside ->
            jdbcTemplate.update(
                "insert into crawl_side (crawlresultat_id, url) values (:cr_id, :url)",
                mapOf("cr_id" to id, "url" to nettside.toString())
            )
        }

        logger.debug(
            "CrawlResultat.Ferdig insert nettsider ferdig. maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id} crid: $id antall nettsider: ${crawlResultat.nettsider.size}"
        )
    }

    private fun saveCrawlresultatFeila(crawlResultat: CrawlResultat.Feila, maalingId: Int) {
        jdbcTemplate.update(
            """
                  insert into crawlresultat (loeysingid, status, maaling_id, sist_oppdatert, feilmelding)
                  values (:loeysingid, :status, :maaling_id, :sist_oppdatert, :feilmelding)
                """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "feilmelding" to crawlResultat.feilmelding
            )
        )
    }

    private fun saveCrawlresultatStarta(crawlResultat: CrawlResultat.Starta, maalingId: Int) {
        jdbcTemplate.update(
            """
                  insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert, lenker_crawla) 
                  values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert, :lenker_crawla)
                """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
                "lenker_crawla" to crawlResultat.framgang.prosessert
            )
        )
    }

    private fun saveCrawlresultatIkkjeStarta(
        crawlResultat: CrawlResultat.IkkjeStarta,
        maalingId: Int,
    ) {
        jdbcTemplate.update(
            """
                  insert into crawlresultat (loeysingid, status, status_url, maaling_id, sist_oppdatert) 
                  values (:loeysingid, :status, :status_url, :maaling_id, :sist_oppdatert)
                """
                .trimIndent(),
            mapOf(
                "loeysingid" to crawlResultat.loeysing.id,
                "status" to status(crawlResultat),
                "status_url" to crawlResultat.statusUrl.toString(),
                "maaling_id" to maalingId,
                "sist_oppdatert" to Timestamp.from(crawlResultat.sistOppdatert),
            )
        )
    }

    private fun crawlresultatAlreadyFinished(crawlResultat: CrawlResultat, maalingId: Int): Boolean {
        val alreadyFinished =
            jdbcTemplate.queryForObject(
                "select count(*) from crawlresultat where loeysingid = :loeysingid and maaling_id = :maaling_id and status = :status_finished",
                mapOf(
                    "loeysingid" to crawlResultat.loeysing.id,
                    "maaling_id" to maalingId,
                    "status_finished" to "ferdig"
                ),
                Int::class.java
            )

        if (alreadyFinished == 1) {
            logger.debug(
                "CrawlResultat.Ferdig hopper over for maalingId: $maalingId loeysingId: ${crawlResultat.loeysing.id}"
            )
            return true
        }
        return false
    }

    private fun status(crawresultat: CrawlResultat): String =
        when (crawresultat) {
            is CrawlResultat.IkkjeStarta -> "ikkje_starta"
            is CrawlResultat.Starta -> "starta"
            is CrawlResultat.Feila -> "feila"
            is CrawlResultat.Ferdig -> "ferdig"
        }

    fun getCrawlResultatNettsider(maalingId: Int, loeysingId: Int): List<URL> =
        jdbcTemplate
            .queryForList(
                """
                select n.url
                from crawl_side n
                    join crawlresultat cr on n.crawlresultat_id = cr.id
                where cr.maaling_id = :maalingId
                    and cr.loeysingid = :loeysingId
              """
                    .trimIndent(),
                mapOf("maalingId" to maalingId, "loeysingId" to loeysingId),
                String::class.java
            )
            .map { url -> URI(url).toURL() }

    fun getCrawlResultatForMaaling(
        maalingId: Int,
        loeysingList: List<Loeysing>,
    ): List<CrawlResultat> {
        val loeysingar = loeysingList.associateBy { it.id }
        return jdbcTemplate.query(
            """
            with agg_nettsider as (
                select crawlresultat_id, count(*) as ant_nettsider
                from crawl_side
                group by crawlresultat_id
            )
            select
                cr.id,
                cr.loeysingid,
                cr.status,
                cr.status_url,
                cr.sist_oppdatert,
                cr.feilmelding,
                cr.lenker_crawla,
                coalesce(an.ant_nettsider, 0) as ant_nettsider,
                m.max_lenker
            from crawlresultat cr
                left join agg_nettsider an on cr.id = an.crawlresultat_id
                join maalingv1 m on m.id = cr.maaling_id
            where
                cr.maaling_id = :maalingId
            order by m.id
                """
                .trimIndent(),
            mapOf("maalingId" to maalingId),
            fun(rs: ResultSet): List<CrawlResultat> {
                val result = mutableListOf<CrawlResultat>()

                while (rs.next()) {
                    val id = rs.getInt("id")
                    val loeysingId = rs.getInt("loeysingid")
                    val loeysing =
                        loeysingar[loeysingId]
                            ?: throw IllegalStateException(
                                "crawlresultat $id er lagra med ei løysing som ikkje finnes."
                            )
                    result.add(toCrawlResultat(rs, loeysing))
                }

                return result.toList()
            })
            ?: throw RuntimeException(
                "fikk `null` da vi forsøkte å hente crawlresultat for måling med id = $maalingId"
            )
    }

    private fun toCrawlResultat(rs: ResultSet, loeysing: Loeysing): CrawlResultat {
        val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
        val status = rs.getString("status")

        val crawlResultat =
            when (status) {
                "ikkje_starta" -> {
                    CrawlResultat.IkkjeStarta(
                        URI(rs.getString("status_url")).toURL(), loeysing, sistOppdatert
                    )
                }

                "starta" -> {
                    val framgang = Framgang(rs.getInt("lenker_crawla"), rs.getInt("max_lenker"))
                    CrawlResultat.Starta(
                        URI(rs.getString("status_url")).toURL(), loeysing, sistOppdatert, framgang
                    )
                }

                "feila" -> {
                    CrawlResultat.Feila(rs.getString("feilmelding"), loeysing, sistOppdatert)
                }

                "ferdig" -> {
                    val statusUrl = rs.getString("status_url")
                    val antallNettsider = rs.getInt("ant_nettsider")
                    CrawlResultat.Ferdig(antallNettsider, URI(statusUrl).toURL(), loeysing, sistOppdatert)
                }

                else -> throw RuntimeException("ukjent status lagret i databasen: $status")
            }

        return crawlResultat
    }

    fun getSideutvalUrlMapKontroll(sideutvalIds: List<Int>): Map<Int, URL> {
        if (sideutvalIds.isEmpty()) {
            return emptyMap()
        }

        return jdbcTemplate
            .query(
                """
        select tsk.sideutval_id, ks.url
        from testgrunnlag_sideutval_kontroll tsk
        join kontroll_sideutval ks on ks.id = tsk.sideutval_id
            where tsk.sideutval_id in (:sideutvalIds)
      """
                    .trimIndent(),
                mapOf("sideutvalIds" to sideutvalIds),
            ) { rs, _ ->
                rs.getInt("sideutval_id") to URI(rs.getString("url")).toURL()
            }
            .toMap()
    }

    fun getSideutvalForMaaling(maalingId: Int): Result<List<Sideutval.Automatisk>> {
        return kotlin.runCatching {
            jdbcTemplate.query(
                """
    select cs.id, cs.crawlresultat_id, cs.url
    from crawl_side cs
    join crawlresultat cr on cr.id = cs.crawlresultat_id
    where cr.maaling_id = :maalingId
  """.trimIndent(), mapOf("maalingId" to maalingId), DataClassRowMapper.newInstance(Sideutval.Automatisk::class.java)
            ).toList()
        }

    }

    fun getSideutvalIdsForMaalingAndUrl(maalingId: Int, url: URL): Result<Int> {
        return kotlin.runCatching {
            jdbcTemplate.queryForList(
                """
                select cs.id
                from crawl_side cs
                join crawlresultat cr on cr.id = cs.crawlresultat_id
                where cr.maaling_id = :maalingId
                and cs.url = :url
              """.trimIndent(), mapOf("maalingId" to maalingId, "url" to url.toString()), Int::class.java
            ).single()
        }
    }
}
