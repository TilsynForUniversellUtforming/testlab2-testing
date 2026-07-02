package no.uutilsynet.testlab2testing.inngaendekontroll.testoverview

import kotlin.collections.distinctBy
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.ResultatManuellKontrollBase
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import org.springframework.stereotype.Service

@Service
class TestOverviewStatisticsService(val testregelClient: TestregelClient) {

  fun getTestingStatusForLoeysing(
      loeysingId: Int,
      testgrunnlagId: Int,
      testresultat: List<ResultatManuellKontroll>,
      testregelIds: List<Int>,
      sideutvalIds: List<Int>
  ): TestStatusCount {
    val numTestregel = testregelIds.size
    val numSider = sideutvalIds.size

    // Sjekk kva som er riktig å vise når ein har retest
    val total = numSider * numTestregel
    val ferdig =
        uniqueTestKeyCount(
            testresultat.filter { it.status == ResultatManuellKontrollBase.Status.Ferdig })
    val underArbeid =
        uniqueTestKeyCount(
            testresultat.filter { it.status == ResultatManuellKontrollBase.Status.UnderArbeid })
    val ikkjeStarta =
        uniqueTestKeyCount(
            testresultat.filter { it.status == ResultatManuellKontrollBase.Status.IkkjePaabegynt })

    val percentagePerSide = progresjonForSideutval(testregelIds, sideutvalIds, testresultat)

    val percectagePerInnholdstype =
        progresjonForInnhaldstype(testregelIds, sideutvalIds, testresultat)

    return TestStatusCount(
        loeysingId,
        testgrunnlagId,
        total,
        ferdig,
        underArbeid,
        ikkjeStarta,
        percentagePerSide,
        percectagePerInnholdstype)
  }

  fun uniqueTestKeyCount(resultat: List<ResultatManuellKontroll>): Int {
    return resultat.distinctBy { it.testregelId to it.sideutvalId }.size
  }

  fun toPerctentage(count: Int, total: Int): Double {
    if (total == 0) return 0.0
    return (count.toDouble() / total.toDouble()) * 100
  }

  fun progresjonForSideutval(
      testregelIds: List<Int>,
      sideutvalIds: List<Int>,
      testresultat: List<ResultatManuellKontroll>
  ): Double {
    val finshedTestsKeys =
        testresultat
            .filter { it.status == ResultatManuellKontrollBase.Status.Ferdig }
            .distinctBy { it.testregelId to it.sideutvalId }
            .toSet()

    val totalTestKeys =
        testregelIds
            .flatMap { testregelId ->
              sideutvalIds.map { sideutvalId -> "${testregelId}_$sideutvalId" }
            }
            .toSet()

    val finishedTestsForSideutval = finshedTestsKeys.intersect(totalTestKeys).size

    return toPerctentage(finishedTestsForSideutval, totalTestKeys.size)
  }

  fun progresjonForInnhaldstype(
      testregelIds: List<Int>,
      sideutvalIds: List<Int>,
      testresultat: List<ResultatManuellKontroll>
  ): Double {
    val innholdstypeGroup =
        testregelClient.getTestregelListFromIds(testregelIds).getOrThrow().groupBy {
          it.innhaldstypeTesting
        }

    val finshedTestsKeys =
        testresultat
            .filter { it.status == ResultatManuellKontrollBase.Status.Ferdig }
            .distinctBy { it.testregelId to it.sideutvalId }
            .toSet()

    val finishedPerInnhaldstype =
        innholdstypeGroup.values.map { entries ->
          val testForInnhaldstype = getTestKeys(entries, sideutvalIds)
          val finishedTestsForInnhaldstype = finshedTestsKeys.intersect(testForInnhaldstype).size
          toPerctentage(finishedTestsForInnhaldstype, testForInnhaldstype.size)
        }

    if (finishedPerInnhaldstype.isEmpty()) return 0.0
    return finishedPerInnhaldstype.average()
  }

  private fun getTestKeys(values: List<Testregel>, sideutvalIds: List<Int>): Set<String> =
      values
          .flatMap { testregel ->
            sideutvalIds.map { sideutvalId -> "${testregel.id}_$sideutvalId" }
          }
          .toSet()
}
