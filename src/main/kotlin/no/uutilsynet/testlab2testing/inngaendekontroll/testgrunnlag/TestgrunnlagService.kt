package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.testregel.Testregel
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Component

@Component
class TestgrunnlagService(val testgrunnlagDAO: TestgrunnlagDAO, val testregelDAO: TestregelDAO) {

  fun createOrUpdate(testgrunnlag: NyttTestgrunnlag): Result<Int> {
    val eksisterende = testgrunnlagDAO.getOpprinneligTestgrunnlag(testgrunnlag.parentId)

    if (eksisterende.isSuccess) {
      val eksisterendeTestgrunnlag =
          testgrunnlagDAO.getTestgrunnlag(eksisterende.getOrThrow()).getOrThrow()

      testgrunnlagDAO
          .updateTestgrunnlag(
              eksisterendeTestgrunnlag.copy(
                  namn = testgrunnlag.namn,
                  testreglar = getTestreglar(testgrunnlag.testreglar),
                  loeysingar = testgrunnlag.loeysingar,
              ))
          .fold(
              onSuccess = {
                return Result.success(it.id)
              },
              onFailure = {
                return Result.failure(it)
              })
    }
    return testgrunnlagDAO.createTestgrunnlag(testgrunnlag)
  }

  fun getTestreglar(testregelIdList: List<Int>): List<Testregel> {
    return testregelDAO.getTestregelList().filter { testregelIdList.contains(it.id) }
  }
}
