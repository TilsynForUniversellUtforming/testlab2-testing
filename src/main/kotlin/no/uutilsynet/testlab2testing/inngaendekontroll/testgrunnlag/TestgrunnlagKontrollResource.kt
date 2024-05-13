package no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag

import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.NyttTestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontroll
import no.uutilsynet.testlab2testing.inngaendekontroll.testgrunnlag.kontroll.TestgrunnlagKontrollDAO
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
@RequestMapping("/testgrunnlag/kontroll")
class TestgrunnlagKontrollResource(val testgrunnlagDAO: TestgrunnlagKontrollDAO) {

  val logger: Logger = LoggerFactory.getLogger(TestgrunnlagKontrollResource::class.java)

  @GetMapping
  fun getTestgrunnlagList(
      @RequestParam kontrollId: Int,
      @RequestParam loeysingId: Int?
  ): ResponseEntity<List<TestgrunnlagKontroll>> {
    return ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId, loeysingId))
  }

  @PostMapping
  fun createTestgrunnlag(@RequestBody testgrunnlag: NyttTestgrunnlagKontroll): ResponseEntity<Int> {
    logger.info(
        "Opprett testgrunnlag for sak ${testgrunnlag.parentId} og loeysinger ${testgrunnlag.sideutval.map { it.loeysingId }}")

    return runCatching { testgrunnlagDAO.createTestgrunnlag(testgrunnlag).getOrThrow() }
        .fold(
            onSuccess = { id ->
              logger.info("Oppretta testgrunnlag med id $id")
              ResponseEntity.created(location(id)).build()
            },
            onFailure = {
              logger.error("Feil ved oppretting av testgrunnlag", it)
              ResponseEntity.internalServerError().build()
            })
  }

  @GetMapping("list/{kontrollId}")
  fun listTestgrunnlagForSak(
      @PathVariable kontrollId: Int
  ): ResponseEntity<List<TestgrunnlagKontroll>> =
      ResponseEntity.ok(testgrunnlagDAO.getTestgrunnlagForKontroll(kontrollId, null))

  @GetMapping("/{id}")
  fun getTestgrunnlag(@PathVariable id: Int): ResponseEntity<TestgrunnlagKontroll> {
    return testgrunnlagDAO
        .getTestgrunnlag(id)
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ResponseEntity.notFound().build() })
  }

  @PutMapping("/{id}")
  fun updateTestgrunnlag(
      @PathVariable id: Int,
      @RequestBody testgrunnlag: TestgrunnlagKontroll
  ): ResponseEntity<TestgrunnlagKontroll> {
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
