package no.uutilsynet.testlab2testing.testresultat.aggregering.mappers

import no.uutilsynet.testlab2.constants.TestresultatUtfall
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testregel.model.TestregelAggregate
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSideDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerSuksesskriteriumDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregeringPerTestregelDB
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatSide
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatSuksesskriterium
import no.uutilsynet.testlab2testing.testresultat.aggregering.AggregertResultatTestregel
import no.uutilsynet.testlab2testing.testresultat.aggregering.GjennomsnittTestresultat
import no.uutilsynet.testlab2testing.testresultat.aggregering.ResultatPerTestregelPerSide
import no.uutilsynet.testlab2testing.testresultat.aggregering.TalUtfall
import org.springframework.stereotype.Service
import java.net.URL

@Service
class AggregeringToDTOMapper(
    private val testregelCache: TestregelCache,
    private val kravregisterClient: KravregisterClient,
    private val sideutvalDAO: SideutvalDAO
) {

    fun aggregertResultatTestregelToDTO(
        aggregertResultatTestregel: AggregertResultatTestregel
    ): AggregeringPerTestregelDB {

        val testregel = testregelCache.getTestregelByKey(aggregertResultatTestregel.testregelId)

        return AggregeringPerTestregelDB(
            aggregertResultatTestregel.maalingId,
            aggregertResultatTestregel.loeysing.id,
            testregel.id,
            testregel.krav.id,
            aggregertResultatTestregel.fleireSuksesskriterium.map {
                kravregisterClient.getKravIdFromSuksesskritterium(it)
            },
            aggregertResultatTestregel.talElementSamsvar,
            aggregertResultatTestregel.talElementBrot,
            aggregertResultatTestregel.talElementVarsel,
            aggregertResultatTestregel.talElementIkkjeForekomst,
            aggregertResultatTestregel.talSiderSamsvar,
            aggregertResultatTestregel.talSiderBrot,
            aggregertResultatTestregel.talSiderIkkjeForekomst,
            aggregertResultatTestregel.testregelGjennomsnittlegSideSamsvarProsent,
            aggregertResultatTestregel.testregelGjennomsnittlegSideBrotProsent,
            null
        )
    }

    fun aggregerteResultatSideTODTO(
        aggregertResultatSide: AggregertResultatSide
    ): AggregeringPerSideDB {
        return AggregeringPerSideDB(
            aggregertResultatSide.maalingId,
            aggregertResultatSide.loeysing.id,
            aggregertResultatSide.sideUrl,
            aggregertResultatSide.sideNivaa,
            aggregertResultatSide.gjennomsnittligBruddProsentTR,
            aggregertResultatSide.talElementSamsvar,
            aggregertResultatSide.talElementBrot,
            aggregertResultatSide.talElementVarsel,
            aggregertResultatSide.talElementIkkjeForekomst,
            null
        )
    }

    fun aggregertResultatSuksesskritieriumToDTO(
        aggregertResultatSuksesskriterium: AggregertResultatSuksesskriterium
    ): AggregeringPerSuksesskriteriumDB {

        return AggregeringPerSuksesskriteriumDB(
            aggregertResultatSuksesskriterium.maalingId,
            aggregertResultatSuksesskriterium.loeysing.id,
            kravregisterClient.getKravIdFromSuksesskritterium(
                aggregertResultatSuksesskriterium.suksesskriterium
            ),
            aggregertResultatSuksesskriterium.talSiderSamsvar,
            aggregertResultatSuksesskriterium.talSiderBrot,
            aggregertResultatSuksesskriterium.talSiderIkkjeForekomst,
            null
        )
    }

    fun aggregeringPerTestregelDB(
        it: Map.Entry<Int, List<ResultatManuellKontroll>>
    ): AggregeringPerTestregelDB {
        val testresultat = it.value
        val talElementUtfall = countElementUtfall(testresultat)

        val (talSiderBrot, talSiderSamsvar, talSiderIkkjeForekomst) = countSideUtfall(testresultat)

        val suksesskriterium = getKravIdFraTestregel(it.key)

        val gjennomsnittTestresultat = calculateTestregelGjennomsnitt(testresultat)

        return AggregeringPerTestregelDB(
            null,
            testresultat.first().loeysingId,
            it.key,
            suksesskriterium,
            listOf(suksesskriterium),
            talElementUtfall.talSamsvar,
            talElementUtfall.talBrot,
            talElementUtfall.talVarsel,
            talElementUtfall.talIkkjeForekomst,
            talSiderSamsvar,
            talSiderBrot,
            talSiderIkkjeForekomst,
            gjennomsnittTestresultat.testregelGjennomsnittlegSideSamsvarProsent,
            gjennomsnittTestresultat.testregelGjennomsnittlegSideBrotProsent,
            testresultat.first().testgrunnlagId
        )
    }

    fun createAggregeringPerSuksesskriteriumDTO(
        testresultatForSak: List<ResultatManuellKontroll>
    ): List<AggregeringPerSuksesskriteriumDB> {
        return testresultatForSak
            .groupBy { it.kravId() }
            .entries
            .map {
                val testresultat = it.value
                val (talSiderBrot, talSiderSamsvar, talSiderIkkjeForekomst) =
                    countSideUtfall(testresultat)
                AggregeringPerSuksesskriteriumDB(
                    null,
                    testresultat.first().loeysingId,
                    testresultat.first().kravId(),
                    talSiderSamsvar,
                    talSiderBrot,
                    talSiderIkkjeForekomst,
                    testresultat.first().testgrunnlagId
                )
            }
    }

    fun createAggregeringPerSideDTO(
        testresultatList: List<ResultatManuellKontroll>
    ): List<AggregeringPerSideDB> {
        val testresultatMap = testresultatList.groupBy { it.sideutvalId }

        val sideutvalIdUrlMap: Map<Int, URL> =
            sideutvalDAO.getSideutvalUrlMapKontroll(testresultatMap.keys.toList())

        // Alle sideutvalIder skal referere til en gyldig url
        require(testresultatMap.keys.containsAll(sideutvalIdUrlMap.keys)) {
            "Ugyldige nettsider i testresultat"
        }

        return testresultatMap
            .mapKeys { sideutvalIdUrlMap[it.key] }
            .filterKeys { it != null }
            .entries
            .map { entry ->
                requireNotNull(entry.key)
                val testresultat = entry.value

                AggregeringPerSideDB(
                    null,
                    testresultat.first().loeysingId,
                    entry.key!!,
                    entry.key!!.path.split("/").size,
                    0.0,
                    testresultat.count { it.elementResultat == TestresultatUtfall.samsvar },
                    testresultat.count { it.elementResultat == TestresultatUtfall.brot },
                    0,
                    testresultat.count { it.elementResultat == TestresultatUtfall.ikkjeForekomst },
                    testresultat.first().testgrunnlagId
                )
            }
    }

    private fun countElementUtfall(values: List<ResultatManuellKontroll>): TalUtfall {
        val talElementBrot = values.count { it.elementResultat == TestresultatUtfall.brot }
        val talElementSamsvar = values.count { it.elementResultat == TestresultatUtfall.samsvar }
        val talElementIkkjeForekomst =
            values.count { it.elementResultat == TestresultatUtfall.ikkjeForekomst }
        val talElementVarsel = values.count { it.elementResultat == TestresultatUtfall.varsel }
        val talElementIkkjeTesta = values.count { it.elementResultat == TestresultatUtfall.ikkjeTesta }
        return TalUtfall(
            talBrot = talElementBrot,
            talSamsvar = talElementSamsvar,
            talIkkjeForekomst = talElementIkkjeForekomst,
            talVarsel = talElementVarsel,
            talIkkjeTesta = talElementIkkjeTesta
        )
    }

    private fun countSideUtfall(testresultat: List<ResultatManuellKontroll>): TalUtfall {
        var talSiderBrot = 0
        var talSiderSamsvar = 0
        var talSiderIkkjeForekomst = 0
        var talSiderVarsel = 0
        var talSiderIkkjeTesta = 0

        testresultat
            .groupBy { it.sideutvalId }
            .entries
            .forEach { _ ->
                when (calculateUtfall(testresultat.map { it.elementResultat })) {
                    TestresultatUtfall.brot -> talSiderBrot += 1
                    TestresultatUtfall.samsvar -> talSiderSamsvar += 1
                    TestresultatUtfall.ikkjeForekomst -> talSiderIkkjeForekomst += 1
                    TestresultatUtfall.varsel -> talSiderVarsel += 1
                    TestresultatUtfall.ikkjeTesta -> talSiderIkkjeTesta += 1
                }
            }
        return TalUtfall(
            talBrot = talSiderBrot,
            talSamsvar = talSiderSamsvar,
            talIkkjeForekomst = talSiderIkkjeForekomst,
            talVarsel = talSiderVarsel,
            talIkkjeTesta = talSiderIkkjeTesta
        )
    }

    fun calculateUtfall(utfall: List<TestresultatUtfall?>): TestresultatUtfall {
        return when {
            utfall.contains(TestresultatUtfall.brot) -> TestresultatUtfall.brot
            utfall.contains(TestresultatUtfall.varsel) -> TestresultatUtfall.varsel
            utfall.contains(TestresultatUtfall.samsvar) -> TestresultatUtfall.samsvar
            else -> TestresultatUtfall.ikkjeForekomst
        }
    }

    fun calculateTestregelGjennomsnitt(
        values: List<ResultatManuellKontroll>
    ): GjennomsnittTestresultat {
        val resultatPerTestregelPerSide: List<ResultatPerTestregelPerSide> =
            values.groupBy { it.sideutvalId }.entries.map { processPrSideutval(it.value) }

        return gjennomsnittTestresultat(resultatPerTestregelPerSide)
    }

    private fun gjennomsnittTestresultat(
        resultatPerTestregelPerSide: List<ResultatPerTestregelPerSide>
    ): GjennomsnittTestresultat {
        var talSiderMedForekomst = 0
        var summertBrotprosent = 0.0
        var summertSamsvarprosent = 0.0

        resultatPerTestregelPerSide.forEach {
            summertBrotprosent += addIfNotIkkjeForekomst(it.brotprosentTrSide, it.ikkjeForekomst)
            summertSamsvarprosent += addIfNotIkkjeForekomst(it.samsvarsprosentTrSide, it.ikkjeForekomst)
            talSiderMedForekomst += addIfNotIkkjeForekomst(1.0, it.ikkjeForekomst).toInt()
        }
        val testregelGjennomsnittlegSideBrot =
            (summertBrotprosent / talSiderMedForekomst).takeUnless { it.isNaN() }
        val testregelGjennomsnittlegSideSamsvar =
            (summertSamsvarprosent / talSiderMedForekomst).takeUnless { it.isNaN() }

        return GjennomsnittTestresultat(
            testregelGjennomsnittlegSideSamsvar, testregelGjennomsnittlegSideBrot
        )
    }

    fun processPrSideutval(values: List<ResultatManuellKontroll>): ResultatPerTestregelPerSide {
        val talElementUtfall = countElementUtfall(values)

        val ikkjeForekomst = talElementUtfall.talIkkjeForekomst > 0

        if (ikkjeForekomst) {
            return ResultatPerTestregelPerSide(
                brotprosentTrSide = 0.0, samsvarsprosentTrSide = 0.0, ikkjeForekomst = true
            )
        }

        return ResultatPerTestregelPerSide(
            brotprosentTrSide =
                (talElementUtfall.talBrot.toDouble() /
                        (talElementUtfall.talBrot + talElementUtfall.talSamsvar).toDouble()),
            samsvarsprosentTrSide =
                (talElementUtfall.talSamsvar.toDouble() /
                        (talElementUtfall.talBrot + talElementUtfall.talSamsvar).toDouble()),
            ikkjeForekomst = false
        )
    }

    fun ResultatManuellKontroll.kravId(): Int {
        return getKravIdFraTestregel(this.testregelId)
    }

    fun getKravIdFraTestregel(id: Int): Int {
        return getTestregel(id).krav.id
    }

    fun getTestregel(testregelId: Int): TestregelAggregate {
        return testregelCache.getTestregelById(testregelId)
    }

    private fun addIfNotIkkjeForekomst(value: Double, ikkjeForekomst: Boolean): Double {
        return value.takeIf { !ikkjeForekomst } ?: 0.0
    }

}