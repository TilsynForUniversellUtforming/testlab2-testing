package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelClient
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.TestregelService
import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatDB
import no.uutilsynet.testlab2testing.testregel.krav.KravregisterClient
import no.uutilsynet.testlab2testing.testresultat.TestresultatDAO
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.springframework.stereotype.Service
import kotlin.math.min

@Service
class AutomatiskResultatService(
    private val maalingService: MaalingService,
    private val testkoeyringDAO: TestkoeyringDAO,
    resultatDAO: ResultatDAO,
    kravregisterClient: KravregisterClient,
    testregelClient: TestregelClient,
    testresultatDAO: TestresultatDAO,
    private val testresultatDBConverter: TestresultatDBConverter,
) : KontrollResultatService(resultatDAO, kravregisterClient, testresultatDAO, testregelClient) {

  @Observed(name = "AutomatiskResultatService.getResultatForKontroll")
  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int
  ): List<TestresultatDetaljert> {
      val maalingId = maalingService.getMaalingForKontroll(kontrollId)
      return if(testresultatDAO.hasResultInDB(maalingId, loeysingId)){
          getDetaljertResultatForKontroll(kontrollId, loeysingId, testregelId, size, pageNumber)
      } else {
          val resultat = getResultatForMaaling(maalingId, loeysingId).filter {
              filterByTestregel(it.testregelId, listOf(testregelId))
          }
              resultat.subList(pageNumber * size, min((pageNumber + 1) * size, resultat.size))
      }
  }

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int
  ): List<TestresultatDetaljert> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return if (testresultatDAO.hasResultInDB(maalingId, loeysingId)) {
      getAutomatiskTestresultatMaaling(maalingId, loeysingId)
    } else {
      getResultatForMaaling(maalingId, loeysingId)
    }
  }

  fun getDetaljertResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int
  ): List<TestresultatDetaljert> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return getAutomatiskTestresultatMaaling(maalingId, loeysingId, testregelId, size, pageNumber)
  }

  private fun getAutomatiskTestresultatMaaling(
      maalingId: Int,
      loeysingId: Int,
      testregelId: Int,
      size: Int,
      pageNumber: Int
  ): List<TestresultatDetaljert> {
    val resultat =
        testresultatDAO.listBy(
            maalingId = maalingId, loeysingId = loeysingId, testregelId, size, (pageNumber) * size)
    return testresultatDBConverter.mapTestresults(resultat, maalingId, loeysingId)
  }

  @Observed(name = "AutomatiskResultatService.getAutomatiskTestresultatMaaling")
  fun getAutomatiskTestresultatMaaling(
      maalingId: Int,
      loeysingId: Int
  ): List<TestresultatDetaljert> {
    val resultat = testresultatDAO.listBy(maalingId = maalingId, loeysingId = loeysingId)
    return testresultatDBConverter.mapTestresults(resultat, maalingId, loeysingId)
  }

  private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
    val testresultat: List<TestResultat> =
        maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId).getOrThrow().map {
          it as TestResultat
        }
    return testresultat
  }

  @Observed(name = "AutomatiskResultatService.getResultatForMaaling")
  fun getResultatForMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
    return getAutotesterTestresultat(maalingId, loeysingId).map {
      testresultatDetaljertMaaling(it, maalingId)
    }
  }

  private fun testresultatDetaljertMaaling(
      it: TestResultat,
      maalingId: Int
  ): TestresultatDetaljert {
    val testregel = testregelClient.getTestregelByKey(it.testregelId)
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

    override fun getTestresulatDetaljertForKrav(
        kontrollId: Int,
        loeysingId: Int,
        kravId: Int,
        size: Int,
        pageNumber: Int
    ): List<TestresultatDetaljert> {
        val testreglar = getTestreglarForKrav(kravId).map { it.id }
        val maalingId = maalingService.getMaalingForKontroll(kontrollId)
        val resultat = testresultatDAO.listBy(maalingId, loeysingId, testreglar, size, pageNumber)

        return if(testresultatDAO.hasResultInDB(maalingId, loeysingId)){
            return testresultatDBConverter.mapTestresults(resultat, maalingId, loeysingId)
        } else {
            getResultatForMaaling(maalingId, loeysingId).filter {
                filterByTestregel(it.testregelId, testreglar)
            }
        }
    }

    override fun getKontrollResultat(kontrollId: Int): List<ResultatLoeysingDTO> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return getKontrollResultatMaaling(maalingId)
  }

  override fun getBrukararForTest(kontrollId: Int): List<String> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return testkoeyringDAO.getTestarTestkoeyringar(maalingId).map { it.namn }.distinct()
  }


    override fun getTalBrotForKontrollLoeysingTestregel(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int
    ): Result<Int> {
   return runCatching {
       val maalingId = maalingService.getMaalingForKontroll(kontrollId)
       if (testresultatDAO.hasResultInDB(maalingId, loeysingId)) {
           testresultatDAO.getTalBrotForKontrollLoeysingTestregel(loeysingId, testregelId, null, maalingId).getOrThrow()
       } else {
           getResultatForMaaling(maalingId, loeysingId).count { filterByTestregel(it.testregelId, listOf(testregelId)) }
       }
   }

    }

    override fun getTalBrotForKontrollLoeysingKrav(kontrollId: Int,
                                                   loeysingId: Int,
                                                   kravId: Int) : Result<Int> {
        val testregelIds = getTestreglarForKrav(kravId).map { it.id }
        val maalingId = maalingService.getMaalingForKontroll(kontrollId)
        return testresultatDAO.getTalBrotForKontrollLoeysingKrav(loeysingId, testregelIds, null, maalingId)
    }
}
