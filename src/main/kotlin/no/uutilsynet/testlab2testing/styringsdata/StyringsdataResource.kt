package no.uutilsynet.testlab2testing.styringsdata

import no.uutilsynet.testlab2testing.forenkletkontroll.logger
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

  @PostMapping
  fun createStyringsdata(@RequestBody styringsdata: Styringsdata): ResponseEntity<Int> =
      styringsdataDAO
          .createStyringsdata(styringsdata)
          .fold(
              { id -> ResponseEntity.created(location(id)).build() },
              {
                logger.error("Feil ved oppretting av testresultat", it)
                ResponseEntity.internalServerError().build()
              })

  @PutMapping("{id}")
  fun updateStyringsdata(@PathVariable("id") id: Int, @RequestBody styringsdata: Styringsdata) =
      styringsdataDAO.updateStyringsdata(id, styringsdata)

  @GetMapping
  fun listStyringsdataForKontroll(
      @RequestParam("kontrollId") kontrollId: Int
  ): List<StyringsdataListElement> = styringsdataDAO.listStyringsdataForKontroll(kontrollId)

  @GetMapping("{id}")
  fun getStyringsdata(
      @PathVariable("id") id: Int,
  ): Styringsdata? {
    return styringsdataDAO.getStyringsdata(id).firstOrNull()?.let { styringsdata ->
      Styringsdata(
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
  }

  private fun location(id: Int) =
      ServletUriComponentsBuilder.fromCurrentRequest().path("/$id").buildAndExpand(id).toUri()
}
