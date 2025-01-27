package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import no.uutilsynet.testlab2testing.inngaendekontroll.testresultat.Bilde
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.*
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
      bildeService
          .createBilde(testresultatId, bilder)
          .fold(
              { ResponseEntity.noContent().build() },
              {
                logger.error("Feil ved opplasting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @DeleteMapping("{testresultatId}/{bildeId}")
  fun deleteBilde(@PathVariable testresultatId: Int, @PathVariable bildeId: Int) =
      bildeService
          .deleteBilder(testresultatId, bildeId)
          .fold(
              { ResponseEntity.noContent().build<Unit>() },
              {
                logger.error("Kunne ikkje slette bilde", it)
                ResponseEntity.internalServerError().build<Unit>()
              })

  @GetMapping("/{testresultatId}")
  fun getBildeListForTestresultat(@PathVariable testresultatId: Int): ResponseEntity<List<Bilde>> =
      bildeService
          .listBildeForTestresultat(testresultatId)
          .fold(
              { ResponseEntity.ok(it) },
              {
                logger.error("Feil ved henting av bilder", it)
                ResponseEntity.internalServerError().build()
              })

  @CacheEvict(value = ["bildeCache"], allEntries = true)
  @Scheduled(fixedRateString = "\${blobstorage.sasttl}")
  fun emptyBildeCache() {
    logger.debug("Tømmer bildeCache")
  }

  @GetMapping("sti/{bildesti}")
  fun getBilde(@PathVariable("bildesti") bildesti: String): ResponseEntity<String> {
    return ResponseEntity.ok(
        bildeService.getBilde(bildesti).inputStream.readAllBytes().toString(Charsets.UTF_8))
  }
}
