package no.uutilsynet.testlab2testing.common.resultutils

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagDAO
import org.springframework.stereotype.Service

@Service
class ResultUtils(val maalingDAO: MaalingDAO, val testgrunnlagDAO: TestgrunnlagDAO) {

  fun getKontrollIds(kontrollId: Int): KontrollIds {
    val maalingId = maalingDAO.getMaalingForKontroll(kontrollId).getOrNull()
    val testgrunnlagId = testgrunnlagDAO.getTestgrunnlagIdForKontroll(kontrollId).getOrNull()
    return KontrollIds(kontrollId, testgrunnlagId, maalingId)
  }
}

data class KontrollIds(val kontrollId: Int, val testgrunnlagId: Int?, val maalingId: Int?) {

  fun id(): Int {
    return testgrunnlagId ?: maalingId ?: throw IllegalArgumentException("No id found")
  }
}
