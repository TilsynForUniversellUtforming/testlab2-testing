package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.inngaendekontroll.sak.Sak
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/testgrunnlag")
class TestgrunnlagResource(val testgrunnlagDAO: TestgrunnlagDAO) {

  val logger: Logger = LoggerFactory.getLogger(TestgrunnlagResource::class.java)

  @GetMapping
  fun getTestgrunnlagList(
      @RequestParam sakId: Int,
      @RequestParam loeysingId: Int?
  ): ResponseEntity<List<Testgrunnlag>> {
    return ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForSak(sakId, loeysingId))
  }

  @PostMapping
  fun createTestgrunnlag(@RequestBody testgrunnlag: NyttTestgrunnlag): ResponseEntity<Int> {
    logger.info(
        "Opprett testgrunnlag for sak ${testgrunnlag.parentId} og loeysing ${testgrunnlag.loeysing}")

    val eksisterende =
        testgrunnlagDAO.getTestgrunnlagForSak(
            testgrunnlag.parentId!!, testgrunnlag.loeysing.loeysingId)
    return if (eksisterende.isEmpty()) {
      runCatching { testgrunnlagDAO.createTestgrunnlag(testgrunnlag).getOrThrow() }
          .fold(
              onSuccess = { id ->
                logger.info("Oppretta testgrunnlag med id $id")
                ResponseEntity.created(location(id)).build()
              },
              onFailure = {
                logger.error("Feil ved oppretting av testgrunnlag", it)
                ResponseEntity.badRequest().build()
              })
    } else {
      ResponseEntity.created(location(eksisterende.first().id)).build()
    }
  }

  @GetMapping("list/{sakId}")
  fun listTestgrunnlagForSak(@PathVariable sakId: Int): ResponseEntity<List<Testgrunnlag>> =
      ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForSak(sakId, null))

  @GetMapping("/{id}")
  fun getTestgrunnlag(@PathVariable id: Int): ResponseEntity<Testgrunnlag> {
    return testgrunnlagDAO
        .getTestgrunnlag(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @PutMapping("/{id}")
  fun updateTestgrunnlag(
      @PathVariable id: Int,
      @RequestBody testgrunnlag: Testgrunnlag
  ): ResponseEntity<Testgrunnlag> {
    require(testgrunnlag.id == id) { "id i URL-en og id er ikkje den same" }
    return testgrunnlagDAO
        .updateTestgrunnlag(testgrunnlag)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @DeleteMapping("/{id}")
  fun deleteTestgrunnlag(@PathVariable id: Int): ResponseEntity<Unit> {
    return runCatching { testgrunnlagDAO.deleteTestgrunnlag(id) }
        .fold(
            onSuccess = { ResponseEntity.noContent().build() },
            onFailure = {
              logger.error("Feil ved sletting av testgrunnlag", it)
              ResponseEntity.notFound().build()
            })
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()
}

data class NyttTestgrunnlag(
    val parentId: Int?,
    val namn: String?,
    val type: Testgrunnlag.TestgrunnlagType,
    val loeysing: Sak.Loeysing,
    val testreglar: List<Int>
)
