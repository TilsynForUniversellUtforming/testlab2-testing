package no.uutilsynet.testlab2testing.sideutval.crawling

import io.micrometer.observation.annotation.Observed
import java.net.URL
import org.slf4j.LoggerFactory

@Observed(name = "SideutvalCache")
class SideutvalCache(sideutvalDAO: SideutvalDAO, maalingId: Int, loeysingId: Int) {

  val logger = LoggerFactory.getLogger(SideutvalCache::class.java)

  private val cacheKey: MutableMap<String, Int> = mutableMapOf()
  private val cacheId: MutableMap<Number, URL> = mutableMapOf()

  init {
    logger.debug("Init SideutvalCache for maalingId: $maalingId")
    sideutvalDAO.getSideutvalForMaalingLoeysing(maalingId, loeysingId).getOrThrow().forEach {
      cacheKey[it.url.toString()] = it.id
      cacheId[it.id] = it.url
    }
  }

  fun getSideutvalId(url: URL): Int {
    return cacheKey[url.toString()]
        ?: throw NoSuchElementException("No side url found for url: $url")
  }

  @Observed(name = "sideutvalcache.getsideutvalurl")
  fun getSideutvalUrl(id: Number): URL {
    return cacheId[id] ?: throw NoSuchElementException("No side url found for id: $id")
  }
}
