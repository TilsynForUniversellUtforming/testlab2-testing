package no.uutilsynet.testlab2testing.loeysing

import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.validateNamn
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
    val loeysingar: List<Loeysing> =
        nyttUtval.loeysingList.map {
          val namn = validateNamn(it.namn).getOrThrow()
          val url = URL(it.url)
          val orgnummer = validateOrgNummer(it.orgnummer).getOrThrow()

          val foundLoeysing = loeysingDAO.findLoeysingByURLAndOrgnummer(url, orgnummer)
          if (foundLoeysing == null) {
            val id = loeysingDAO.createLoeysing(namn, url, orgnummer)
            Loeysing(id, namn, url, orgnummer)
          } else {
            foundLoeysing
          }
        }
    logger.atInfo().log("lagrar eit nytt utval med namn $nyttUtval.namn")
    val utvalId = utvalDAO.createUtval(nyttUtval.namn, loeysingar.map { it.id }).getOrThrow()
    return ResponseEntity.created(URI("/v1/utval/$utvalId")).build()
  }

  @GetMapping("{id}")
  fun getUtval(@PathVariable id: Int): Utval {
    return utvalDAO.getUtval(id).getOrThrow()
  }

  data class NyttUtval(val namn: String, val loeysingList: List<Loeysing.External>)
}
