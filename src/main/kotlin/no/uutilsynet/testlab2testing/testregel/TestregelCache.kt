package no.uutilsynet.testlab2testing.testregel

import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Service
class TestregelCache(testregelService: TestregelService) {
    private val cacheKey: MutableMap<String, TestregelKrav> = mutableMapOf()
    private val cacheIds: MutableMap<Int,TestregelKrav> = mutableMapOf()


    init {
      testregelService.getTestregelKravList().forEach { testregel ->
            cacheKey[testregel.testregelId] = testregel
            cacheIds[testregel.id] = testregel
        }
    }

    /*@Observed(name = "testregelcache.getBykey")*/
    fun getTestregelByKey(testregelKey: String): TestregelKrav {
        return cacheKey[testregelKey] ?: throw NoSuchElementException("Test regel not found: $testregelKey")
    }

    @Observed(name = "testregelcache.getbyid")
    fun getTestregelById(testregelId: Int): TestregelKrav {
        return cacheIds[testregelId] ?: throw NoSuchElementException("Test regel not found for id: $testregelId")
    }

  }