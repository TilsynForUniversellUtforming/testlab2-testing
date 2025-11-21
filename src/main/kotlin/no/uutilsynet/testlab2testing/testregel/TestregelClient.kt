package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.springframework.stereotype.Service

@Service
class TestregelClient(
    private val testregelService: TestregelService,
    private val testregelCache: TestregelCache
) {
  fun getTestregelById(testregelId: Int) = testregelCache.getTestregelById(testregelId)

  fun getTestregelByKey(testregelKey: String): TestregelKrav =
      testregelCache.getTestregelByKey(testregelKey)

    fun getTestregelListFromIds(testregelIdList: List<Int>): List<Testregel> {
    return testregelService.getTestregelListFromIds(testregelIdList)
  }

  fun getTestregelList(): List<Testregel> {
    return testregelService.getTestregelList()
  }

  fun getInnhaldstypeForTesting() = testregelService.getInnhaldstypeForTesting()
}
