package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.forenkletkontroll.MaalingService
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.TestgrunnlagService
import no.uutilsynet.testlab2testing.kontroll.KontrollDAO
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.springframework.stereotype.Service

@Service
class TestregelClient(
    private val testregelService: TestregelService,
    private val testregelCache: TestregelCache,
    private val maalingService: MaalingService,
    private val kontrollDAO: KontrollDAO,
    private val testgrunnlagService: TestgrunnlagService
) {
  fun getTestregelById(testregelId: Int) = testregelCache.getTestregelById(testregelId)

  fun getTestregelByKey(testregelKey: String): TestregelKrav =
      testregelCache.getTestregelByKey(testregelKey)

  fun deleteTestregel(testregelId: Int): Int {
    // Sjekk om testregelen er i bruk i noen kontroller
    val kontrollerMedTestregel = kontrollDAO.hasKontrollerTestregel(testregelId)
    check(!kontrollerMedTestregel) {
      "Kan ikkje slette testregel med id $testregelId fordi den er i bruk i kontroller."
    }

    // Sjekk om testregelen er i bruk i noe testgrunnlag
    val testgrunnlagMedTestregel = testgrunnlagService.hasTestgrunnlagTestregel(testregelId)
    check(!testgrunnlagMedTestregel) {
      "Kan ikkje slette testregel med id $testregelId fordi den er i bruk i testgrunnlag."
    }

    val maalingMedTestregel = maalingService.hasMaalingTestregel(testregelId)
    check(!maalingMedTestregel) {
      "Kan ikkje slette testregel med id $testregelId fordi den er i bruk i maaling."
    }

    return testregelService.deleteTestregel(testregelId)
  }

  fun getTestregelListFromIds(testregelIdList: List<Int>): List<Testregel> {
    return testregelService.getTestregelListFromIds(testregelIdList)
  }

  fun getTestregelList(): List<Testregel> {
    return testregelService.getTestregelList()
  }

  fun getInnhaldstypeForTesting() = testregelService.getInnhaldstypeForTesting()
}
