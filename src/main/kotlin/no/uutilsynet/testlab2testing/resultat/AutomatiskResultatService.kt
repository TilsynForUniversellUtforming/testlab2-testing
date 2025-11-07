package no.uutilsynet.testlab2testing.resultat

import io.micrometer.observation.annotation.Observed
import no.uutilsynet.testlab2testing.brukar.BrukarService
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.krav.KravregisterClient
import no.uutilsynet.testlab2testing.sideutval.crawling.Sideutval
import no.uutilsynet.testlab2testing.sideutval.crawling.SideutvalDAO
import no.uutilsynet.testlab2testing.testing.automatisk.TestResultat
import no.uutilsynet.testlab2testing.testing.automatisk.TestkoeyringDAO
import no.uutilsynet.testlab2testing.testregel.TestregelCache
import no.uutilsynet.testlab2testing.testregel.TestregelService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.stream.Collectors

@Service
class AutomatiskResultatService(
    val maalingService: MaalingService,
    val testkoeyringDAO: TestkoeyringDAO,
    resultatDAO: ResultatDAO,
    testregelService: TestregelService,
    kravregisterClient: KravregisterClient,
    val testresultatDBConverter: TestresultatDBConverter,
    val testresultatDAO: TestresultatDAO

) : KontrollResultatService(resultatDAO, kravregisterClient, testregelService) {

  @Observed(name= "AutomatiskResultatService.getResultatForKontroll")
  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int,
      testregelId: Int,
      limit: Int,
      offset: Int
  ): List<TestresultatDetaljert> {
    return getDetaljertResultatForKontroll(kontrollId,loeysingId,testregelId,limit,offset)
  }

  override fun getResultatForKontroll(
      kontrollId: Int,
      loeysingId: Int
  ): List<TestresultatDetaljert> {
    val maalingId = maalingService.getMaalingForKontroll(kontrollId)
    return getAutomatiskTestresultatMaaling(maalingId, loeysingId)
  }

    fun getDetaljertResultatForKontroll(
        kontrollId: Int,
        loeysingId: Int,
        testregelId: Int,
        limit: Int,
        offset: Int
    ): List<TestresultatDetaljert> {
      val maalingId = maalingService.getMaalingForKontroll(kontrollId)
      return getAutomatiskTestresultatMaaling(maalingId, loeysingId,testregelId,limit,offset)
    }

    private fun getAutomatiskTestresultatMaaling(
        maalingId: Int,
        loeysingId: Int,
        testregelId: Int,
        limit: Int,
        offset: Int
    ): List<TestresultatDetaljert> {
        val resultat = testresultatDAO.listBy(maalingId = maalingId, loeysingId = loeysingId,testregelId,limit,offset)
        return testresultatDBConverter.mapTestresults(resultat,maalingId)
    }

    @Observed(name = "AutomatiskResultatService.getAutomatiskTestresultatMaaling")
    fun getAutomatiskTestresultatMaaling(maalingId: Int, loeysingId: Int?): List<TestresultatDetaljert> {
        val resultat = testresultatDAO.listBy(maalingId = maalingId, loeysingId = loeysingId)
        return testresultatDBConverter.mapTestresults(resultat,maalingId)

    }

  private fun getAutotesterTestresultat(maalingId: Int, loeysingId: Int?): List<TestResultat> {
    val testresultat: List<TestResultat> =
        maalingService.getTestresultatMaalingLoeysing(maalingId, loeysingId).getOrThrow().map {
          it as TestResultat
        }
    return testresultat
  }

  @Observed(name= "AutomatiskResultatService.getResultatForMaaling")
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

@Component
class TestresultatDBConverter(
    val testregelCache: TestregelCache,
                                val sideutvalDAO: SideutvalDAO,
                                val brukarService: BrukarService,){

    val logger: Logger = LoggerFactory.getLogger(TestresultatDBConverter::class.java)



    @Observed(name = "AutomatiskResultatService.mapTestresults")
    fun mapTestresults(list: List<TestresultatDB> , maalingId: Int): List<TestresultatDetaljert> {
        val sideutvalCache = sideutvalDAO.getSideutvalForMaaling(maalingId).getOrThrow().associateBy { it.id }
        return list.parallelStream().map {  it.toTestresultatDetaljert(sideutvalCache) }.collect(Collectors.toList())
    }

    private fun TestresultatDB.toTestresultatDetaljert(sideutvalCache: Map<Int,Sideutval.Automatisk>): TestresultatDetaljert {
        val testregel = testregelCache.getTestregelById(testregelId)
        val elementOmtale = TestresultatDetaljert.ElementOmtale(
            this.elmentOmtaleHtml,
            this.elementOmtalePointer,
            this.elementOmtaleDescription
        )
        return TestresultatDetaljert(
            this.id,
            this.loeysingId,
            testregelId,
            testregel.testregelId,
            this.maalingId ?: this.testgrunnlagId ?: 0,
            sideutvalCache[this.sideutvalId]?.url ?: throw IllegalStateException("Sideutval not found for id ${this.sideutvalId}"),
            listOf(testregel.kravId.suksesskriterium),
            LocalDateTime.ofInstant(this.testUtfoert, ZoneId.systemDefault()),
            this.elementUtfall,
            this.elementResultat,
            elementOmtale,
           null,
            null,
            null
        )
    }

}
