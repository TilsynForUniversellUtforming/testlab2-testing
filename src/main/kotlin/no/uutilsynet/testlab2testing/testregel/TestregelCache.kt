package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.model.Tema
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TestregelCache(private val testregelClient: TestregelClient) {
  private val cacheKey: MutableMap<String, TestregelKrav> = mutableMapOf()
  private val cacheIds: MutableMap<Int, TestregelKrav> = mutableMapOf()
    private val temaList: MutableMap<Int, Tema> = mutableMapOf()

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

    fun getTemaById(temaId: Int?): Tema {
        if (temaList.isEmpty()) {
            init()
        }
        return temaList[temaId]
            ?: throw NoSuchElementException("Tema not found for id: $temaId")
    }

    fun getTestregelByKravId(kravId: Int): List<TestregelKrav> {
        if (cacheIds.isEmpty()) {
            init()
        }
        return cacheIds.values.filter { it.krav.id == kravId }}


  fun init() {
    testregelClient.getTestregelKravList().getOrThrow()
        .forEach { testregel ->
      cacheKey[testregel.testregelId] = testregel
      cacheIds[testregel.id] = testregel
    }

      testregelClient.getTemaForTestregel().getOrThrow().forEach { tema ->
          temaList[tema.id] = tema
      }
  }


}
