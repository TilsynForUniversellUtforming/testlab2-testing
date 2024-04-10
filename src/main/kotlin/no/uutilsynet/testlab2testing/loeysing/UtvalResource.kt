package no.uutilsynet.testlab2testing.loeysing

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.common.validateURL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("v1/utval")
@Tag(
    name = "Utval",
    description = "API for å lage og hente utval. Eit utval er ei samling med løysingar.")
class UtvalResource(
    @Autowired val utvalDAO: UtvalDAO,
    @Autowired val loeysingsRegisterClient: LoeysingsRegisterClient
) {
  val logger: Logger = LoggerFactory.getLogger(UtvalResource::class.java)

  @Operation(
      summary = "Lagar eit nytt utval",
      description =
          """
Gitt eit namn og ei liste med løysingar, lagar denne metoden eit nytt utval.

Løysingar som ikkje finst frå før, blir lagra i databasen. Løysingar blir samanlikna på organisasjonsnummer og URL.
Dersom ein løysing med same organisasjonsnummer og URL finst frå før, blir den eksisterande løysinga brukt.

[Her finn du eksempelkode i python](https://github.com/TilsynForUniversellUtforming/utval-api-python-example) på korleis ein
kan importere eit utval frå ei CSV-fil eller ein python dataframe med dette API-et.
      """,
      responses =
          [
              ApiResponse(
                  responseCode = "201",
                  description =
                      "Utvalet vart laga, og du finn ei lenke til det nye utvalet i headeren \"Location\""),
              ApiResponse(
                  responseCode = "400",
                  description =
                      "Eit av argumenta var ugyldig. Du finn meir informasjon i *body*.")])
  @PostMapping
  fun createUtval(@RequestBody nyttUtval: NyttUtval): ResponseEntity<Unit> {
    val utvalNamn = validateNamn(nyttUtval.namn).getOrThrow()
    val loeysingList =
        nyttUtval.loeysingList.map {
          val namn = validateNamn(it.namn).getOrThrow()
          val url = validateURL(it.url).getOrThrow()
          val orgnummer = validateOrgNummer(it.orgnummer).getOrThrow()
          Triple(namn, url, orgnummer)
        }

    val loeysingar: List<Loeysing> =
        loeysingList.map { (namn, url, orgnummer) ->
          val sammeOrgnummer = loeysingsRegisterClient.search(orgnummer).getOrThrow()
          val foundLoeysing = sammeOrgnummer.find { sameURL(it.url, url) }
          if (foundLoeysing == null) {
            logger
                .atInfo()
                .log("lagrar ei ny løysing som vi ikkje fann i databasen: $namn, $url, $orgnummer")
            val loeysing = loeysingsRegisterClient.saveLoeysing(namn, url, orgnummer).getOrThrow()
            loeysing
          } else {
            foundLoeysing
          }
        }
    logger.atInfo().log("lagrar eit nytt utval med namn ${nyttUtval.namn}")
    val utvalId = utvalDAO.createUtval(utvalNamn, loeysingar.map { it.id }).getOrThrow()
    return ResponseEntity.created(location(utvalId)).build()
  }

  private fun location(utvalId: UtvalId) =
      ServletUriComponentsBuilder.fromCurrentRequest()
          .path("/{utvalId}")
          .buildAndExpand(utvalId)
          .toUri()

  @Operation(
      summary = "Hentar detaljert informasjon om eit utval.",
      description =
          "Kvart utval inneheld id, namn og ei liste med løysingar. For å finne id-en til eit utval, kan du bruke GET /v1/utval.",
      responses =
          [
              ApiResponse(responseCode = "200", description = "Utvalet vart funne"),
              ApiResponse(responseCode = "404", description = "Utvalet vart ikkje funne")])
  @GetMapping("{id}")
  fun getUtval(@PathVariable id: Int): ResponseEntity<Utval> {
    return fetchUtval(id)
        .map { utval -> ResponseEntity.ok(utval) }
        .getOrElse {
          when (it) {
            is IllegalArgumentException -> ResponseEntity.notFound().build()
            else -> ResponseEntity.internalServerError().build()
          }
        }
  }

  fun fetchUtval(id: Int): Result<Utval> {
    return utvalDAO.getUtval(id).mapCatching {
      val loeysingar = loeysingsRegisterClient.getMany(it.loeysingar).getOrThrow()
      Utval(it.id, it.namn, loeysingar, it.oppretta)
    }
  }

  @Operation(
      summary = "Hentar ei liste med alle utvala.",
      description =
          "Kvart utval er berre beskrive med id og namn. For å hente lista med løysingar i utvalet, bruk GET /v1/utval/{id}.",
      responses =
          [ApiResponse(responseCode = "200", description = "Returnerer ei liste med alle utval")])
  @GetMapping
  fun getUtvalList(): List<UtvalListItem> {
    return utvalDAO.getUtvalList().getOrThrow()
  }

  @DeleteMapping("{id}")
  fun deleteUtval(@PathVariable id: Int): ResponseEntity<Unit> {
    utvalDAO.deleteUtval(id)
    return ResponseEntity.ok().build()
  }

  data class NyttUtval(val namn: String, val loeysingList: List<Loeysing.External>)
}
