package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/testresultat")
class TestResultatResource(val testResultatDAO: TestResultatDAO) {
  val logger: Logger = getLogger(TestResultatResource::class.java)

  @PostMapping("")
  fun createTestResultat(
      @RequestBody createTestResultat: CreateTestResultat
  ): ResponseEntity<Unit> =
      runCatching { testResultatDAO.save(createTestResultat).getOrThrow() }
          .fold(
              { id -> ResponseEntity.created(location(id)).build() },
              { ResponseEntity.internalServerError().build() })

  @PostMapping("/{id}/svar")
  fun createSvar(
      @PathVariable id: Int,
      @RequestBody body: Map<String, String>
  ): ResponseEntity<Unit> {
    return validateSvar(body)
        .mapCatching { stegOgSvar -> testResultatDAO.saveSvar(id, stegOgSvar) }
        .fold({ ResponseEntity.ok().build() }, { ResponseEntity.internalServerError().build() })
  }

  @GetMapping("/{id}")
  fun getTestResultat(@PathVariable id: Int): ResponseEntity<ResultatManuellKontroll> {
    return testResultatDAO
        .getTestResultat(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = {
              logger.error("Feil ved henting av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @PutMapping("/{id}")
  fun updateTestResultat(
      @PathVariable id: Int,
      @RequestBody testResultat: ResultatManuellKontroll.UnderArbeid
  ): ResponseEntity<Unit> {
    require(testResultat.id == id) { "id i URL-en og id i dei innsendte dataene er ikkje den same" }
    return testResultatDAO
        .update(testResultat)
        .fold(
            onSuccess = { ResponseEntity.ok().build() },
            onFailure = {
              logger.error("Feil ved oppdatering av testresultat", it)
              ResponseEntity.internalServerError().build()
            })
  }

  private fun validateSvar(stegOgSvar: Map<String, String>): Result<Pair<String, String>> {
    return kotlin.runCatching {
      val steg = stegOgSvar["steg"] ?: throw IllegalArgumentException("steg mangler")
      val svar = stegOgSvar["svar"] ?: throw IllegalArgumentException("svar mangler")
      Pair(steg, svar)
    }
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()

  data class CreateTestResultat(
      val sakId: Int,
      val loeysingId: Int,
      val testregelId: Int,
      val nettsideId: Int,
      val elementOmtale: String? = null,
      val elementResultat: String? = null,
      val elementUtfall: String? = null,
      val testVartUtfoert: Instant? = null
  )
}
