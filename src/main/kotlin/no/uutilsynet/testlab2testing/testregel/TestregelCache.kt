package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TestregelCache(private val testregelService: TestregelService) {
  private val cacheKey: MutableMap<String, TestregelKrav> = mutableMapOf()
  private val cacheIds: MutableMap<Int, TestregelKrav> = mutableMapOf()

  val logger = LoggerFactory.getLogger(this::class.java)

  /*@Observed(name = "testregelcache.getBykey")*/
  fun getTestregelByKey(testregelKey: String): TestregelKrav {
    if (cacheIds.isEmpty()) {
      init()
    }

    return cacheKey[testregelKey]
        ?: throw NoSuchElementException("Testregel not found: $testregelKey")
  }

  /*@Observed(name = "testregelcache.getbyid")*/
  fun getTestregelById(testregelId: Int): TestregelKrav {
    if (cacheIds.isEmpty()) {
      init()
    }
    return cacheIds[testregelId]
        ?: throw NoSuchElementException("Testregel not found for id: $testregelId")
  }

  fun init() {
    testregelService.getTestregelKravList().forEach { testregel ->
      cacheKey[testregel.testregelId] = testregel
      cacheIds[testregel.id] = testregel
    }
  }
}
