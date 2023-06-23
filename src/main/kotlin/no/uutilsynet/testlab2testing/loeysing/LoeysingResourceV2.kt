package no.uutilsynet.testlab2testing.loeysing

import java.net.URL
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.executeWithErrorHandling
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil.handleErrors
import no.uutilsynet.testlab2testing.common.validateOrgNummer
import no.uutilsynet.testlab2testing.maaling.validateNamn
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("v2/loeysing")
class LoeysingResourceV2(val loeysingDAO: LoeysingDAO) {

  data class CreateLoeysingDTO(val namn: String, val url: String, val orgnummer: String)

  @PostMapping
  fun createLoeysing(@RequestBody dto: CreateLoeysingDTO) =
      runCatching {
            val namn = validateNamn(dto.namn).getOrThrow()
            val url = URL(dto.url)
            val orgnummer = validateOrgNummer(dto.orgnummer).getOrThrow()

            loeysingDAO.createLoeysing(namn, url, orgnummer)
          }
          .fold(
              { id ->
                val location = locationForId(id)
                ResponseEntity.created(location).build()
              },
              { exception -> handleErrors(exception) })

  @PutMapping
  fun updateLoeysing(@RequestBody loeysing: Loeysing) = executeWithErrorHandling {
    // v2 skal ikke godta manglende orgnummer.
    validateOrgNummer(loeysing.orgnummer).getOrThrow()
    loeysingDAO.updateLoeysing(loeysing)
  }
}
