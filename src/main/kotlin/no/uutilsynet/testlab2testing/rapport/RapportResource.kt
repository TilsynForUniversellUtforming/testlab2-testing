package no.uutilsynet.testlab2testing.rapport

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rapport")
class RapportResource(val rapportService: RapportService) {

  @GetMapping(
      "/kontroll/{kontrollId}/loeysing/{loeysingId}",
      produces = ["application/vnd.openxmlformats-officedocument.wordprocessingml.document"])
  fun lagRapport(
      @PathVariable kontrollId: Int,
      @PathVariable loeysingId: Int,
      response: HttpServletResponse
  ) {

    response.setHeader("Content-disposition", "attachment;filename=tilsynsrapport.docx")
    response.contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    val rapport = rapportService.opprettRapport(kontrollId, loeysingId)

    val wordRapport = rapportService.createWordRapport(rapport)
    response.outputStream.write(wordRapport)
    response.outputStream.flush()
  }
}
