package no.uutilsynet.testlab2testing.testregel

import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Service
class TestregelCache(val testregelService: TestregelService) {
  private val cacheKey: MutableMap<String, TestregelKrav> = mutableMapOf()
  private val cacheIds: MutableMap<Int, TestregelKrav> = mutableMapOf()


  /*@Observed(name = "testregelcache.getBykey")*/
  fun getTestregelByKey(testregelKey: String): TestregelKrav {
    init()
    return cacheKey[testregelKey]
        ?: throw NoSuchElementException("Test regel not found: $testregelKey")
  }

  @Observed(name = "testregelcache.getbyid")
  fun getTestregelById(testregelId: Int): TestregelKrav {
    init()
    return cacheIds[testregelId]
        ?: throw NoSuchElementException("Test regel not found for id: $testregelId")
  }

  fun init() {
    if(cacheKey.isEmpty() || cacheIds.isEmpty()) {
      testregelService.getTestregelKravList().forEach { testregel ->
        cacheKey[testregel.testregelId] = testregel
        cacheIds[testregel.id] = testregel
      }
    }
  }

}
