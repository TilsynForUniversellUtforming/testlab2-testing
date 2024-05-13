package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll

import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class TestgrunnlagServiceKontroll(
    val testgrunnlagDAO: TestgrunnlagKontrollDAO,
    val kontrollDAO: KontrollDAO,
    val testregelDAO: TestregelDAO,
) {

  fun createOrUpdate(testgrunnlag: NyttTestgrunnlagKontroll): Result<Int> {
    val opprinneligTestgrunnlag = testgrunnlagDAO.getOpprinneligTestgrunnlag(testgrunnlag.parentId)

    return if (opprinneligTestgrunnlag.isSuccess) {
      opprinneligTestgrunnlag
          .mapCatching { testgrunnlagDAO.getTestgrunnlag(it) }
          .mapCatching { updateExisting(it.getOrThrow(), testgrunnlag) }
          .map { it.getOrThrow().id }
    } else {
      testgrunnlagDAO.createTestgrunnlag(testgrunnlag)
    }
  }

  private fun updateExisting(
      eksisterendeTestgrunnlag: TestgrunnlagKontroll,
      testgrunnlag: NyttTestgrunnlagKontroll
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
                      testgrunnlag.parentId, testgrunnlag.sideutval.map { it.loeysingId }),
          ))
}
