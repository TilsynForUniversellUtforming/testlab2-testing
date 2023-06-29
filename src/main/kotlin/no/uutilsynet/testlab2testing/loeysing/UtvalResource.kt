package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import no.uutilsynet.testlab2testing.common.validateNamn
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.common.validateURL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v1/utval")
class UtvalResource(@Autowired val utvalDAO: UtvalDAO, @Autowired val loeysingDAO: LoeysingDAO) {
  val logger: Logger = LoggerFactory.getLogger(UtvalResource::class.java)

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
          val foundLoeysing = loeysingDAO.findLoeysingByURLAndOrgnummer(url, orgnummer)
          if (foundLoeysing == null) {
            logger
                .atInfo()
                .log("lagrar ei ny løysing som vi ikkje fann i databasen: $namn, $url, $orgnummer")
            val id = loeysingDAO.createLoeysing(namn, url, orgnummer)
            Loeysing(id, namn, url, orgnummer)
          } else {
            foundLoeysing
          }
        }
    logger.atInfo().log("lagrar eit nytt utval med namn ${nyttUtval.namn}")
    val utvalId = utvalDAO.createUtval(utvalNamn, loeysingar.map { it.id }).getOrThrow()
    return ResponseEntity.created(URI("/v1/utval/$utvalId")).build()
  }

  @GetMapping("{id}")
  fun getUtval(@PathVariable id: Int): Utval {
    return utvalDAO.getUtval(id).getOrThrow()
  }

  @GetMapping
  fun getUtvalList(): List<UtvalListItem> {
    return utvalDAO.getUtvalList().getOrThrow()
  }

  data class NyttUtval(val namn: String, val loeysingList: List<Loeysing.External>)
}
