package no.uutilsynet.testlab2testing.resultat

import org.springframework.stereotype.Component

@Component
class ResultatCalculator {

  fun talElementBrot(resultat: List<ResultatLoeysingDTO>): Int =
      resultat.sumOf { it.talElementBrot }

  fun talElementSamsvar(resultat: List<ResultatLoeysingDTO>): Int =
      resultat.sumOf { it.talElementSamsvar }

  fun talTestaElementFraDto(resultat: List<ResultatLoeysingDTO>): Int =
      talElementBrot(resultat) + talElementSamsvar(resultat)

  fun talTestaElementFraLoeysing(resultat: List<ResultatLoeysing>): Int =
      resultat.sumOf { it.talElementBrot } + resultat.sumOf { it.talElementSamsvar }

  fun scoreOrZero(resultat: List<ResultatLoeysingDTO>): Double {
    val scores = scoreValuesFraDto(resultat)
    return if (scores.isEmpty()) 0.0 else scores.average()
  }

  fun scoreAverageFraLoeysing(resultat: List<ResultatLoeysing>): Double =
      scoreValuesFraLoeysing(resultat).average()

  fun scoreAndElements(resultat: List<ResultatLoeysingDTO>): Triple<Double, Int, Int> {
    val score = scoreOrZero(resultat)
    return Triple(score, talElementBrot(resultat), talElementSamsvar(resultat))
  }

  fun erIkkjeForekomst(talElementBrot: Int, talElementSamsvar: Int): Boolean {
    return talElementBrot == 0 && talElementSamsvar == 0
  }

  private fun scoreValuesFraDto(resultat: List<ResultatLoeysingDTO>): List<Double> {
    return resultat
        .filter { !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar) }
        .map { it.score }
  }

  private fun scoreValuesFraLoeysing(resultat: List<ResultatLoeysing>): List<Double> {
    return resultat
        .filter { !erIkkjeForekomst(it.talElementBrot, it.talElementSamsvar) }
        .map { it.score }
  }
}
