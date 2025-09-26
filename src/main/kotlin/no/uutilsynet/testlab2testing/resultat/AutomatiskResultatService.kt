package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.springframework.stereotype.Service

@Service
class AutomatiskResultatService(
    val maalingService: MaalingService,
    val testkoeyringDAO: TestkoeyringDAO,
    resultatDAO: ResultatDAO,
    testregelService: TestregelService,
    kravregisterClient: KravregisterClient,
) : KontrollResultatService(resultatDAO, kravregisterClient, testregelService) {

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int
  ): List<TestresultatDetaljert> {
    return getResultatForKontroll(kontrollId, loeysingId).filter {
      filterByTestregel(it.testregelId, listOf(testregelId))
    }
  }

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int
  ): List<TestresultatDetaljert> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return getResultatForMaaling(maalingId, loeysingId)
  }

  private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
    val testresultat: List<TestResultat> =
        maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId).getOrThrow().map {
          it as TestResultat
        }
    return testresultat
  }

  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return getAutotesterTestresultat(maalingId, loeysingId).map {
      testresultatDetaljertMaaling(it, maalingId)
    }
  }

  private fun testresultatDetaljertMaaling(
      it: TestResultat,
      maalingId: Int
  ): TestresultatDetaljert {
    val testregel = getTestregelIdFromSchema(it.testregelId)
    return TestresultatDetaljert(
        null,
        it.loeysingId,
        testregel.id,
        testregel.testregelId,
        maalingId,
        it.side,
        it.suksesskriterium,
        it.testVartUtfoert,
        it.elementUtfall,
        it.elementResultat,
        it.elementOmtale,
        null,
        null,
        emptyList())
  }

  private fun getKontrollResultatMaaling(maalingId: Int?): List<ResultatLoeysingDTO> {
    val resultatMaaling =
        maalingId?.let { resultatDAO.getTestresultatMaaling(it) } ?: getAlleResultat()
    return resultatMaaling
  }

  override fun getAlleResultat(): List<ResultatLoeysingDTO> {
    return resultatDAO.getTestresultatMaaling()
  }

  override fun progresjonPrLoeysing(
      testgrunnlagId: Int,
      loeysingar: ResultatService.LoysingList,
  ): Map<Int, Int> {
    return loeysingar.loeysingar.keys.associateWith { 100 }
  }

  override fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return getKontrollResultatMaaling(maalingId)
  }

  override fun getBrukararForTest(kontrollId: Int): List<String> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return testkoeyringDAO.getTestarTestkoeyringar(maalingId).map { it.namn }.distinct()
  }
}
