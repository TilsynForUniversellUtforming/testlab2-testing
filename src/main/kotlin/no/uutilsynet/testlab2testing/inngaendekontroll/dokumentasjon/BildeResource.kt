package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/bilder")
class BildeResource(val bildeService: BildeService) {
  val logger: Logger = LoggerFactory.getLogger(BildeResource::class.java)

  @PostMapping("/{testresultatId}")
  fun createBilde(
      @PathVariable testresultatId: Int,
      @RequestParam("bilder") bilder: List<MultipartFile>
  ): ResponseEntity<Any> =
      runCatching { bildeService.createBilde(testresultatId, bilder) }
          .fold(
              { ResponseEntity.noContent().build() },
              {
                logger.error("Feil ved opplasting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @DeleteMapping("{testresultatId}/{bildeId}")
  fun deleteBilde(@PathVariable testresultatId: Int, @PathVariable bildeId: Int) =
      runCatching {
            bildeService.deleteBilder(testresultatId, bildeId)
            ResponseEntity.noContent().build<Unit>()
          }
          .onFailure {
            logger.error("Kunne ikkje slette bilde", it)
            ResponseEntity.internalServerError().build<Unit>()
          }

  @Cacheable("bildeCache", key = "#testresultatId")
  @GetMapping("/{testresultatId}")
  fun getBildeListForTestresultat(@PathVariable testresultatId: Int): ResponseEntity<List<Bilde>> =
      runCatching { bildeService.getBildeListForTestresultat(testresultatId) }
          .fold(
              { ResponseEntity.ok(it.getOrThrow()) },
              {
                logger.error("Feil ved opplasting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @CacheEvict(value = ["bildeCache"], allEntries = true)
  @Scheduled(fixedRateString = "\${blobstorage.sasttl}")
  fun emptyBildeCache() {
    logger.info("Tømmer bildeCache")
  }
}
