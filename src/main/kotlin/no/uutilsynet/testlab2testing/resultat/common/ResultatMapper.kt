package no.uutilsynet.testlab2testing.resultat.common

import no.uutilsynet.testlab2testing.resultat.ResultatOversiktLoeysing
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregel
import no.uutilsynet.testlab2testing.resultat.ResultatPerTestregelDTO
import no.uutilsynet.testlab2testing.testregel.TestregelCache

object ResultatMapper {
    fun calculateScore(resultLoeysing: List<ResultatPerTestregelDTO>): Double =
        resultLoeysing.filter { filterIkkjeForekomst(it) }.map { it.score }.average()

    fun calculateTalElementBrot(resultLoeysing: List<ResultatPerTestregelDTO>): Int =
        resultLoeysing.sumOf { it.talElementBrot }

    fun calculateTalElementSamsvar(resultLoeysing: List<ResultatPerTestregelDTO>): Int =
        resultLoeysing.sumOf { it.talElementSamsvar }

    fun talTestaElement(result: List<ResultatPerTestregel>): Int =
        result.sumOf { it.talElementBrot } + result.sumOf { it.talElementSamsvar }

    fun talTestaElementDTO(resultLoeysing: List<ResultatPerTestregelDTO>): Int =
        calculateTalElementSamsvar(resultLoeysing) + calculateTalElementSamsvar(resultLoeysing)

    fun filterIkkjeForekomst(it: ResultatPerTestregelDTO): Boolean =
        !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar)

    fun erIkkjeForekomst(talElementBrot: Int, talElementSamsvar: Int): Boolean =
        talElementBrot == 0 && talElementSamsvar == 0

    inline fun <reified T> handleIkkjeForekomstGeneric(
        item: T,
        talElementBrot: Int,
        talElementSamsvar: Int,
        copyWithNullScore: (T) -> T
    ): T =
        if (talElementBrot == 0 && talElementSamsvar == 0) copyWithNullScore(item) else item

    fun handleIkkjeForekomst(resultat: ResultatOversiktLoeysing): ResultatOversiktLoeysing =
        handleIkkjeForekomstGeneric(
            resultat, resultat.talElementBrot, resultat.talElementSamsvar
        ) { it.copy(score = null) }

    fun mapTestregel(result: ResultatPerTestregelDTO, testregelCache: TestregelCache): ResultatPerTestregel {
        val testregel = testregelCache.getTestregelById(result.testregelId)
        return ResultatPerTestregel(
            id = result.id,
            testgrunnlagId = result.testgrunnlagId,
            namn = result.namn,
            typeKontroll = result.typeKontroll,
            testType = result.testType,
            dato = result.dato,
            testar = result.testar,
            loeysingId = result.loeysingId,
            score = result.score,
            talElementSamsvar = result.talElementSamsvar,
            talElementBrot = result.talElementBrot,
            testregelId = result.testregelId,
            testregeltTittel = testregel.namn,
            kravId = testregel.krav.id,
            kravTittel = testregel.krav.tittel
        )
    }

    fun mapResultatOversiktLoeysing(
        result: List<ResultatPerTestregel>,
        loeysingar: LoysingList,
        testregelId: Int
    ) = ResultatOversiktLoeysing(
        result.first().loeysingId,
        loeysingar.getNamn(result.first().loeysingId),
        result.first().typeKontroll,
        result.first().namn,
        result.map { it.testar }.flatten().distinct(),
        result.filter { !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar) }.map { it.score }.average(),
        testregelId,
        result.first().testregeltTittel,
        talTestaElement(result),
        result.sumOf { it.talElementBrot },
        result.sumOf { it.talElementSamsvar }
    )

    fun calculateScoreAndElements(result: List<ResultatPerTestregelDTO>): Triple<Double, Int, Int> {
        val score = result.filter { filterIkkjeForekomst(it) }.map { it.score }.average()
        val talElementBrot = result.sumOf { it.talElementBrot }
        val talElementSamsvar = result.sumOf { it.talElementSamsvar }
        return Triple(score, talElementBrot, talElementSamsvar)
    }

}