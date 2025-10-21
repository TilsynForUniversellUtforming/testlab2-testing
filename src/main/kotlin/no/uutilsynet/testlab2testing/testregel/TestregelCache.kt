package no.uutilsynet.testlab2testing.testregel

import org.springframework.stereotype.Service

@Service
class TestregelCache(testregelService: TestregelService) {
    private val cacheKey: MutableMap<String, Testregel> = mutableMapOf()


    init {
        testregelService.getTestregelList().forEach { testregel -> cacheKey[testregel.testregelId] = testregel }
    }


    fun getTestregelByKey(testregelKey: String): Testregel {
        return cacheKey[testregelKey] ?: throw NoSuchElementException("Test regel not found: $testregelKey")
    }

  }