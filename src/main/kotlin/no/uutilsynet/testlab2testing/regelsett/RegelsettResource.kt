package no.uutilsynet.testlab2testing.regelsett

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import java.net.URI
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.testregel.TestregelDAO
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

@RestController
@RequestMapping("v1/regelsett")
class RegelsettResource(val regelsettDAO: RegelsettDAO, val testregelDAO: TestregelDAO) {

  val logger = LoggerFactory.getLogger(RegelsettResource::class.java)

  @Operation(
      summary = "Opprett eit nytt regelsett",
      description =
          """
      Eit nytt regelsett treng eit namn, ein type, om det skal være standard regelsett og ei liste med testregel-idaar
    """,
      responses =
          [
              ApiResponse(
                  responseCode = "201",
                  description =
                      "Når eit regelsett blir oppretta rikitg, lenke til nytt regelsett er i \"Location\" i header"),
              ApiResponse(
                  responseCode = "400",
                  description =
                      "Feil i request-objektet, til dømes manglande namn eller ugyldige testreglar"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @PostMapping
  fun createRegelsett(@RequestBody regelsett: RegelsettCreate): ResponseEntity<out Any> =
      runCatching {
            val namn = validateNamn(regelsett.namn).getOrThrow()

            val testregelList = testregelDAO.getTestregelList()
            val testregelIdList =
                validateRegelsettTestreglar(
                        regelsett.testregelIdList, regelsett.type, testregelList)
                    .getOrThrow()

            val id =
                regelsettDAO.createRegelsett(
                    RegelsettCreate(
                        namn,
                        regelsett.type,
                        regelsett.standard,
                        testregelIdList,
                    ))

            return ResponseEntity.created(URI("/v1/regelsett/${id}")).build()
          }
          .getOrElse { ex ->
            logger.error("Kunne ikkje opprette regelsett med namn: ${regelsett.namn}")
            handleErrors(ex)
          }

  @Operation(
      summary = "Hent ei liste med regelsett",
      description =
          """
Returnerer ei liste med regelsett, eller ei tom liste om ingen finst. Ein kan spesifisera om ein vil ha inaktive regelsett med parameteren "includeInactive" 
""",
      responses =
          [
              ApiResponse(responseCode = "200", description = "Liste med eksisterande regelsett"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @GetMapping
  fun getRegelsettList(
      @RequestParam(required = false, defaultValue = "false") includeInactive: Boolean
  ): List<RegelsettBase> = regelsettDAO.getList(includeInactive)

  @Operation(
      summary = "Hent eit spesifikt regelsett",
      description = "Skal returnere eit regelsett med oppgitt id",
      responses =
          [
              ApiResponse(responseCode = "200", description = "Valt regelsett"),
              ApiResponse(responseCode = "404", description = "Ingen regelsett funne for gitt id"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @GetMapping("{id}")
  fun getRegelsett(@PathVariable id: Int): ResponseEntity<Regelsett> =
      regelsettDAO.getRegelsett(id)?.let { ResponseEntity.ok(it) }
          ?: ResponseEntity.notFound().build()

  @Operation(
      summary = "Endre eit regelsett",
      description =
          """
        For å oppdatera eit regelsett treng ein id, namn, om det er eit standard regelsett og ei liste med testregel-id-ar. 
        
        Ein kan ikkje endra typa regelsett, eller leggja inn testreglar som er ein annan type enn regelsettet.
      """,
      responses =
          [
              ApiResponse(responseCode = "204", description = "Regelsett er oppdatert"),
              ApiResponse(
                  responseCode = "400",
                  description =
                      "Feil i request-objektet, til dømes manglande namn eller ugyldige testreglar"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @PutMapping
  fun updateRegelsett(@RequestBody regelsett: RegelsettEdit): ResponseEntity<out Any> =
      runCatching {
            val testregelList = testregelDAO.getTestregelList()

            val namn = validateNamn(regelsett.namn)
            val testregelIdList =
                validateRegelsettTestreglar(
                    regelsett.testregelIdList, regelsett.type, testregelList)

            regelsettDAO.updateRegelsett(
                RegelsettEdit(
                    regelsett.id,
                    namn.getOrThrow(),
                    regelsett.type,
                    regelsett.standard,
                    testregelIdList.getOrThrow()))

            ResponseEntity.noContent().build<Unit>()
          }
          .getOrElse { ex ->
            logger.error("Oppdatering av regelsett feila for regelsett id: ${regelsett.id}")
            handleErrors(ex)
          }

  @Operation(
      summary = "Deaktiver eit regelsett",
      description = """
      Man kan deaktivere eit regelsett med en gitt id.
      """,
      responses =
          [
              ApiResponse(responseCode = "204", description = "Regelsett er deaktivert"),
              ApiResponse(responseCode = "500", description = "Andre feil")])
  @DeleteMapping("{id}")
  fun deleteRegelsett(@PathVariable id: Int): ResponseEntity<out Any> =
      runCatching {
            regelsettDAO.deleteRegelsett(id)
            ResponseEntity.noContent().build<Unit>()
          }
          .getOrElse { exception -> handleErrors(exception) }
}
