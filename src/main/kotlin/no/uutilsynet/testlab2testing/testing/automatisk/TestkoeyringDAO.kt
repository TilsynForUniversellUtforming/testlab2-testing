package no.uutilsynet.testlab2testing.testing.automatisk

import no.uutilsynet.testlab2testing.brukar.Brukar
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.forenkletkontroll.Framgang
import no.uutilsynet.testlab2testing.loeysing.Loeysing
import no.uutilsynet.testlab2testing.sideutval.crawling.CrawlResultat
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URL
import java.sql.ResultSet
import java.time.Instant



@Component
class TestkoeyringDAO(val jdbcTemplate: NamedParameterJdbcTemplate,val brukarService: BrukarService) {

    fun getTestKoeyringarForMaaling(
        maalingId: Int,
        crawlResultatMap: Map<Int, CrawlResultat.Ferdig>
    ): List<TestKoeyring> {
        return jdbcTemplate.query<TestKoeyring>(
            """
              select t.id, maaling_id, loeysing_id, status, status_url, sist_oppdatert, feilmelding, t.lenker_testa, url_fullt_resultat, url_brot,url_agg_tr,url_agg_sk,url_agg_side,url_agg_side_tr,url_agg_loeysing, brukar_id
              from "testlab2_testing"."testkoeyring" t
              where maaling_id = :maaling_id
            """
                .trimIndent(),
            mapOf("maaling_id" to maalingId),
            fun(rs: ResultSet, _: Int): TestKoeyring {
                val status = rs.getString("status")
                val loeysingId = rs.getInt("loeysing_id")
                val brukar = getBrukarFromResultSet(rs)

                val crawlResultatForLoeysing = crawlResultatMap[loeysingId]
                    ?: throw RuntimeException("Finner ikkje crawlresultat for loeysing med id = $loeysingId")


                val sistOppdatert = rs.getTimestamp("sist_oppdatert").toInstant()
                return when (status) {
                    "ikkje_starta" -> {
                        ikkjeStarta(crawlResultatForLoeysing.loeysing, sistOppdatert, crawlResultatForLoeysing.antallNettsider, rs, brukar)
                    }
                    "starta" -> {
                        starta(crawlResultatForLoeysing.loeysing, sistOppdatert,crawlResultatForLoeysing.antallNettsider, rs, brukar)
                    }
                    "feila" -> feila(crawlResultatForLoeysing.loeysing, sistOppdatert,  rs, brukar)
                    "ferdig" -> {
                        ferdig(rs, crawlResultatForLoeysing.loeysing, sistOppdatert,crawlResultatForLoeysing.antallNettsider, brukar)
                    }
                    else -> throw RuntimeException("ukjent status $status")
                }
            })
    }

    private fun getBrukarFromResultSet(rs: ResultSet) =
        brukarService.getBrukarById(rs.getInt("brukar_id"))

    private fun feila(
        loeysing: Loeysing,
        sistOppdatert: Instant,
        rs: ResultSet,
        brukar: Brukar?,
    ) =
        TestKoeyring.Feila(
            loeysing, sistOppdatert, rs.getString("feilmelding"), brukar)

    private fun starta(
        loeysing: Loeysing,
        sistOppdatert: Instant,
        antallNettsider: Int,
        rs: ResultSet,
        brukar: Brukar?
    ) =
        TestKoeyring.Starta(
            loeysing,
            sistOppdatert,
            statusUrl(rs),
            Framgang(rs.getInt("lenker_testa"), antallNettsider),
            brukar,
            antallNettsider)

    private fun ikkjeStarta(
        loeysing: Loeysing,
        sistOppdatert: Instant,
        antallNettsider: Int,
        rs: ResultSet,
        brukar: Brukar?
    ) =
        TestKoeyring.IkkjeStarta(
            loeysing,
            sistOppdatert,
            statusUrl(rs),
            brukar,
            antallNettsider)


    private fun ferdig(
        rs: ResultSet,
        loeysing: Loeysing,
        sistOppdatert: Instant,
        antallNettsider: Int,
        brukar: Brukar?
    ): TestKoeyring.Ferdig {
        val urlFulltResultat = rs.getString("url_fullt_resultat")
        val urlBrot = rs.getString("url_brot")
        val urlAggTR = rs.getString("url_agg_tr")
        val urlAggSK = rs.getString("url_agg_sk")
        val urlAggSide = rs.getString("url_agg_side")
        val urlAggSideTR = rs.getString("url_agg_side_tr")
        val urlAggLoeysing = rs.getString("url_agg_loeysing")

        val lenker =
            autoTesterLenker(
                urlFulltResultat, urlBrot, urlAggTR, urlAggSK, urlAggSide, urlAggSideTR, urlAggLoeysing)
        return TestKoeyring.Ferdig(
            loeysing,
            sistOppdatert,
            statusUrl(rs),
            lenker,
            brukar,
            antallNettsider)
    }

    private fun statusUrl(rs: ResultSet): URL = URI(rs.getString("status_url")).toURL()

    private fun autoTesterLenker(
        urlFulltResultat: String?,
        urlBrot: String?,
        urlAggTR: String?,
        urlAggSK: String?,
        urlAggSide: String?,
        urlAggSideTR: String?,
        urlAggLoeysing: String?
    ): AutoTesterClient.AutoTesterLenker? {

        val lenker =
            if (urlFulltResultat != null)
                AutoTesterClient.AutoTesterLenker(
                    verifiedAutotestlenker(urlFulltResultat),
                    verifiedAutotestlenker(urlBrot),
                    verifiedAutotestlenker(urlAggTR),
                    verifiedAutotestlenker(urlAggSK),
                    verifiedAutotestlenker(urlAggSide),
                    verifiedAutotestlenker(urlAggSideTR),
                    verifiedAutotestlenker(urlAggLoeysing))
            else null
        return lenker
    }

    private fun verifiedAutotestlenker(autotesterLenke: String?): URL {
        if (autotesterLenke == null) {
            throw RuntimeException("autotestlenke er null")
        }
        return URI(autotesterLenke).toURL()
    }



}