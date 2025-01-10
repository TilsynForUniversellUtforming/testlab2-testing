package no.uutilsynet.testlab2testing.styringsdata

import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
@RequestMapping("/styringsdata")
class StyringsdataResource(val styringsdataDAO: StyringsdataDAO) {

  private val logger = LoggerFactory.getLogger(StyringsdataResource::class.java)

  @GetMapping
  fun findStyringsdataForKontroll(@RequestParam("kontrollId") kontrollId: Int): StyringsdataResult {
    val styringsdataKontroll = styringsdataDAO.findStyringsdataKontroll(kontrollId)
    val styringsdataLoeysing = styringsdataDAO.findStyringsdataLoeysing(kontrollId)

    return StyringsdataResult(styringsdataKontroll?.id, styringsdataLoeysing)
  }

  @GetMapping("{stryingsdataType}/{stryingsdataId}")
  fun getStyringsdata(
      @PathVariable("stryingsdataType") stryingsdataType: StyringsdataType,
      @PathVariable("stryingsdataId") stryingsdataId: Int,
  ): Styringsdata? {
    return if (stryingsdataType == StyringsdataType.loeysing) {
      styringsdataDAO.getStyringsdataLoeysing(stryingsdataId).firstOrNull()?.let { styringsdata ->
        Styringsdata.Loeysing(
            id = styringsdata.id,
            loeysingId = styringsdata.kontrollId,
            kontrollId = styringsdata.loeysingId,
            ansvarleg = styringsdata.ansvarleg,
            oppretta = styringsdata.oppretta,
            frist = styringsdata.frist,
            reaksjon = styringsdata.reaksjon,
            paaleggReaksjon = styringsdata.paaleggReaksjon,
            paaleggKlageReaksjon = styringsdata.paaleggKlageReaksjon,
            botReaksjon = styringsdata.botReaksjon,
            botKlageReaksjon = styringsdata.botKlageReaksjon,
            paalegg = styringsdata.paaleggId?.let { styringsdataDAO.getPaalegg(it) },
            paaleggKlage = styringsdata.paaleggKlageId?.let { styringsdataDAO.getKlage(it) },
            bot = styringsdata.botId?.let { styringsdataDAO.getBot(it) },
            botKlage = styringsdata.botKlageId?.let { styringsdataDAO.getKlage(it) },
            sistLagra = styringsdata.sistLagra)
      }
    } else {
      styringsdataDAO.getStyringsdataKontroll(stryingsdataId).firstOrNull()
    }
  }

  @PostMapping
  fun createStyringsdata(@RequestBody styringsdata: Styringsdata): ResponseEntity<Int> =
      when (styringsdata) {
        is Styringsdata.Loeysing -> {
          styringsdataDAO.createStyringsdataLoeysing(styringsdata)
        }
        is Styringsdata.Kontroll -> {
          styringsdataDAO.createStyringsdataKontroll(styringsdata)
        }
        else -> {
          logger.error("Kan ikkje opprette styringsdata")
          throw IllegalStateException("Kan ikkje opprette denne typen styringsdata")
        }
      }.fold(
          { id -> ResponseEntity.created(location(styringsdata, id)).build() },
          {
            logger.error("Feil ved oppretting av styringsdata", it)
            ResponseEntity.internalServerError().build()
          })

  @PutMapping("{stryingsdataType}/{stryingsdataId}")
  fun updateStyringsdata(
      @PathVariable("stryingsdataType") stryingsdataType: StyringsdataType,
      @PathVariable("stryingsdataId") stryingsdataId: Int,
      @RequestBody styringsdata: Styringsdata
  ) =
      when (styringsdata) {
        is Styringsdata.Loeysing -> {
          styringsdataDAO.updateStyringsdataLoeysing(stryingsdataId, styringsdata)
        }
        is Styringsdata.Kontroll -> {
          styringsdataDAO.updateStyringsdataKontroll(stryingsdataId, styringsdata)
        }
        else -> {
          logger.error("Kan ikkje oppdatere styringsdata med id $stryingsdataId")
          throw IllegalStateException("Kan ikkje oppdatere styringsdata")
        }
      }

  private fun location(styringsdata: Styringsdata, id: Int): URI {
    val type =
        if (styringsdata is Styringsdata.Loeysing) StyringsdataType.loeysing
        else StyringsdataType.kontroll
    return ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/${type.name}/$id")
        .buildAndExpand(id)
        .toUri()
  }
}

data class StyringsdataResult(
    val styringsdataKontrollId: Int?,
    val styringsdataLoeysing: List<StyringsdataListElement> = emptyList()
)
