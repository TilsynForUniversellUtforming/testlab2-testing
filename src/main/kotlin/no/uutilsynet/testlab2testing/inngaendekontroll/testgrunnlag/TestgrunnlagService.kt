package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.forenkletkontroll.SideutvalDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class TestgrunnlagService(
    val testgrunnlagDAO: TestgrunnlagDAO,
    val testregelDAO: TestregelDAO,
    val sideutvalDAO: SideutvalDAO
) {

  fun createOrUpdate(testgrunnlag: NyttTestgrunnlag): Result<Int> {
    val eksisterende = testgrunnlagDAO.getOpprinneligTestgrunnlag(testgrunnlag.parentId)

    return if (eksisterende.isSuccess) {
      eksisterende
          .mapCatching { testgrunnlagDAO.getTestgrunnlag(it) }
          .mapCatching { updateExisting(it.getOrThrow(), testgrunnlag) }
          .map { it.getOrThrow().id }
    } else {
      testgrunnlagDAO.createTestgrunnlag(
          testgrunnlag.copy(
              loeysingar = oppdaterNettsideIdar(testgrunnlag.loeysingar, testgrunnlag.parentId)))
    }
  }

  private fun updateExisting(
      eksisterendeTestgrunnlag: Testgrunnlag,
      testgrunnlag: NyttTestgrunnlag
  ) =
      testgrunnlagDAO.updateTestgrunnlag(
          eksisterendeTestgrunnlag.copy(
              namn = testgrunnlag.namn,
              testreglar = getTestreglar(testgrunnlag.testreglar),
              loeysingar =
                  oppdaterNettsideIdar(testgrunnlag.loeysingar, eksisterendeTestgrunnlag.sakId),
          ))

  fun getTestreglar(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getTestregelList().filter { testregelIdList.contains(it.id) }
  }

  fun oppdaterNettsideIdar(loeysingar: List<Sak.Loeysing>, parentId: Int): List<Sak.Loeysing> {
    return loeysingar.map {
      val nettsider = sideutvalDAO.findNettsiderBySakAndLoeysing(parentId, it.loeysingId)
      Sak.Loeysing(it.loeysingId, nettsider)
    }
  }
}
