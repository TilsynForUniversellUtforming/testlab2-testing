package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class TestgrunnlagServiceKontroll(
    val testgrunnlagDAO: TestgrunnlagDAO,
    val kontrollDAO: KontrollDAO,
    val testregelDAO: TestregelDAO,
) {

  fun createOrUpdate(testgrunnlag: NyttTestgrunnlag): Result<Int> {
    val opprinneligTestgrunnlag =
        testgrunnlagDAO.getOpprinneligTestgrunnlag(testgrunnlag.kontrollId)

    return if (opprinneligTestgrunnlag.isSuccess) {
      opprinneligTestgrunnlag
          .mapCatching { testgrunnlagDAO.getTestgrunnlag(it) }
          .mapCatching { updateExisting(it.getOrThrow(), testgrunnlag) }
          .map { it.getOrThrow().id }
    } else {
      testgrunnlagDAO.createTestgrunnlag(testgrunnlag)
    }
  }

  fun kontrollHasTestresultat(kontrollId: Int): Boolean =
      testgrunnlagDAO.kontrollHasTestresultat(kontrollId)

  private fun updateExisting(
      eksisterendeTestgrunnlag: TestgrunnlagKontroll,
      testgrunnlag: NyttTestgrunnlag
  ) =
      testgrunnlagDAO.updateTestgrunnlag(
          eksisterendeTestgrunnlag.copy(
              namn = testgrunnlag.namn,
              testreglar =
                  testregelDAO.getTestregelList().filter {
                    testgrunnlag.testregelIdList.contains(it.id)
                  },
              sideutval =
                  kontrollDAO.findSideutvalByKontrollAndLoeysing(
                      testgrunnlag.kontrollId, testgrunnlag.sideutval.map { it.loeysingId }),
          ))
}
