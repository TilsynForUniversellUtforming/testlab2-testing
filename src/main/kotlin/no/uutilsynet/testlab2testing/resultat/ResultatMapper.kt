package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.testregel.TestregelCache
import org.springframework.stereotype.Component

@Component
class ResultatMapper(
    private val testregelCache: TestregelCache,
    private val resultatCalculator: ResultatCalculator,
) {

  fun toResultatTema(entry: Map.Entry<Int, List<ResultatLoeysingDTO>>): ResultatTema {
    val testregel = testregelCache.getTestregelById(entry.key)
    val (score, talElementBrot, talElementSamsvar) =
        resultatCalculator.scoreAndElements(entry.value)

    return ResultatTema(
        temaNamn = testregel.tema?.tema ?: "Utan tema",
        score = score,
        talTestaElement = talElementBrot + talElementSamsvar,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talVarsel = 0,
        talElementIkkjeForekomst = 0,
    )
  }

  fun sumResultatTema(entry: Map.Entry<String, List<ResultatTema>>): ResultatTema {
    val items = entry.value

    return ResultatTema(
        temaNamn = entry.key,
        score = items.mapNotNull { it.score }.average(),
        talTestaElement = items.sumOf { it.talTestaElement },
        talElementBrot = items.sumOf { it.talElementBrot },
        talElementSamsvar = items.sumOf { it.talElementSamsvar },
        talVarsel = 0,
        talElementIkkjeForekomst = 0,
    )
  }

  fun toResultatKrav(entry: Map.Entry<Int, List<ResultatLoeysingDTO>>): ResultatKrav {
    val testregel = testregelCache.getTestregelById(entry.key)
    val (score, talElementBrot, talElementSamsvar) =
        resultatCalculator.scoreAndElements(entry.value)

    return ResultatKrav(
        kravId = testregel.krav.id,
        suksesskriterium = testregel.krav.suksesskriterium,
        score = score,
        talTestaElement = talElementBrot + talElementSamsvar,
        talElementBrot = talElementBrot,
        talElementSamsvar = talElementSamsvar,
        talElementVarsel = 0,
        talElementIkkjeForekomst = 0,
    )
  }

  fun sumResultatKrav(entry: Map.Entry<Int, List<ResultatKrav>>): ResultatKrav {
    val items = entry.value

    return ResultatKrav(
        kravId = entry.key,
        suksesskriterium = items.first().suksesskriterium,
        score = items.mapNotNull { it.score }.average(),
        talTestaElement = items.sumOf { it.talTestaElement },
        talElementBrot = items.sumOf { it.talElementBrot },
        talElementSamsvar = items.sumOf { it.talElementSamsvar },
        talElementVarsel = 0,
        talElementIkkjeForekomst = 0,
    )
  }

  fun toResultatOversiktLoeysing(
      resultat: List<ResultatLoeysingDTO>,
      loeysingar: LoysingList,
  ): List<ResultatOversiktLoeysing> {
    return resultat
        .map(::toResultatLoeysing)
        .groupBy { it.testregelId }
        .map { (testregelId, result) ->
          handleIkkjeForekomst(mapResultatOversiktLoeysing(result, loeysingar, testregelId))
        }
  }

  private fun toResultatLoeysing(result: ResultatLoeysingDTO): ResultatLoeysing {
    val testregel = testregelCache.getTestregelById(result.testregelId)

    return ResultatLoeysing(
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
        kravTittel = testregel.krav.tittel,
    )
  }

  private fun mapResultatOversiktLoeysing(
      result: List<ResultatLoeysing>,
      loeysingar: LoysingList,
      testregelId: Int,
  ): ResultatOversiktLoeysing {
    return ResultatOversiktLoeysing(
        loeysingId = result.first().loeysingId,
        loeysingNamn = loeysingar.getNamn(result.first().loeysingId),
        typeKontroll = result.first().typeKontroll,
        kontrollNamn = result.first().namn,
        testar = result.flatMap { it.testar }.distinct(),
        score = resultatCalculator.scoreAverageFraLoeysing(result),
        testregelId = testregelId,
        testregelTittel = result.first().testregeltTittel,
        talTestaElement = resultatCalculator.talTestaElementFraLoeysing(result),
        talElementBrot = result.sumOf { it.talElementBrot },
        talElementSamsvar = result.sumOf { it.talElementSamsvar },
    )
  }

  private fun handleIkkjeForekomst(resultat: ResultatOversiktLoeysing): ResultatOversiktLoeysing {
    return handleIkkjeForekomstGeneric(
        item = resultat,
        talElementBrot = resultat.talElementBrot,
        talElementSamsvar = resultat.talElementSamsvar,
    ) {
      it.copy(score = null)
    }
  }

  private inline fun <reified T> handleIkkjeForekomstGeneric(
      item: T,
      talElementBrot: Int,
      talElementSamsvar: Int,
      copyWithNullScore: (T) -> T,
  ): T {
    return if (resultatCalculator.erIkkjeForekomst(talElementBrot, talElementSamsvar)) {
      copyWithNullScore(item)
    } else {
      item
    }
  }
}
