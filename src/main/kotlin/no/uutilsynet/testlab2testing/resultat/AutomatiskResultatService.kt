package no.uutilsynet.testlab2testing.resultat

import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingDAO
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
import org.springframework.stereotype.Service

@Service
class AutomatiskResultatService(
    val maalingDAO: MaalingDAO,
    val maalingService: MaalingService,
    resultatDAO: ResultatDAO,
    testregelDAO: TestregelDAO,
    kravregisterClient: KravregisterClient
) : KontrollResultatService(resultatDAO, testregelDAO, kravregisterClient) {

  fun getResultatForAutomatiskKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int
  ): List<TestresultatDetaljert> {
    val maalingId = getMaalingForKontroll(kontrollId)
    return getResultatForMaaling(maalingId, loeysingId).filter {
      filterByTestregel(it.testregelId, listOf(testregelId))
    }
  }

  private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
    val testresultat: List<TestResultat> =
        maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId).getOrThrow().map {
          it as TestResultat
        }
    return testresultat
  }

  private fun getMaalingForKontroll(kontrollId: Int): Int {
    val maalingId =
        maalingDAO.getMaalingIdFromKontrollId(kontrollId)
            ?: throw RuntimeException("Fant ikkje maalingId for kontrollId $kontrollId")
    return maalingId
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

  fun getKontrollResultatMaaling(maalingId: Int?): List<ResultatLoeysingDTO> {
    val resultatMaaling =
        maalingId?.let { resultatDAO.getTestresultatMaaling(it) }
            ?: getKontrollResultatAlleMaalingar()
    return resultatMaaling
  }

  fun getKontrollResultatAlleMaalingar(): List<ResultatLoeysingDTO> {
    return resultatDAO.getTestresultatMaaling()
  }

  override fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    val maalingId = maalingDAO.getMaalingIdFromKontrollId(kontrollId)
    return getKontrollResultatMaaling(maalingId)
  }

  override fun getBrukararForTest(kontrollId: Int): List<String> {
    val maalingId =
        maalingDAO.getMaalingIdFromKontrollId(kontrollId)
            ?: throw RuntimeException("Fant ikkje maalingId for kontrollId $kontrollId")
    return maalingDAO.getTestarTestkoeyringarForMaaling(maalingId).map { it.namn }.distinct()
  }
}
