package no.uutilsynet.testlab2testing.ekstern.resultat

import jakarta.servlet.http.HttpServletResponse
import java.io.ByteArrayOutputStream
import no.uutilsynet.testlab2testing.common.ErrorHandlingUtil
import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BildeService
import org.apache.poi.ooxml.POIXMLProperties
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ekstern/tester")
class EksternResultatResource(
    @Autowired val eksternResultatService: EksternResultatService,
    @Autowired val publiseringService: EksternResultatPubliseringService,
    @Autowired val bildeService: BildeService,
) {

  private val logger = LoggerFactory.getLogger(EksternResultatResource::class.java)

  @GetMapping
  fun findTestForOrgNr(
      @RequestParam("orgnr") orgnr: String?,
      @RequestParam("searchparam") searchparam: String?,
  ): ResponseEntity<Any?> {
    logger.debug("Henter tester for orgnr $orgnr")

    return try {
      val result = findTests(searchparam, orgnr)
      ResponseEntity.ok(result)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build()
    } catch (e: IllegalArgumentException) {
      logger.warn(e.message)
      ResponseEntity.status(HttpStatusCode.valueOf(422)).body(e.message)
    } catch (e: Exception) {
      logger.error(e.message)
      ResponseEntity.badRequest().build()
    }
  }

  private fun findTests(searchparam: String?, orgnr: String?): TestListElementEkstern {
    return if (searchparam != null) {
      eksternResultatService.findTestForOrgNr(searchparam).getOrThrow()
    } else if (orgnr != null) {
      eksternResultatService.findTestForOrgNr(orgnr).getOrThrow()
    } else {
      throw IllegalArgumentException("Mangler søkeparameter")
    }
  }

  @GetMapping("rapport/{rapportId}")
  fun getResultatRapport(@PathVariable rapportId: String): ResponseEntity<out Any> {
    return kotlin
        .runCatching { eksternResultatService.getResultatForRapport(rapportId) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}")
  fun getResultRapportLoeysing(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
  ): ResponseEntity<out Any> {
    return kotlin
        .runCatching { eksternResultatService.getRapportForLoeysing(rapportId, loeysingId) }
        .fold(
            onSuccess = { ResponseEntity.ok(it) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/tema")
  fun getResultatPrTema(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
  ): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrTema(rapportId, loeysingId)
        .fold(
            onSuccess = { resultatTema -> ResponseEntity.ok(resultatTema) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/krav")
  fun getResultatPrKrav(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
  ): ResponseEntity<out Any> {
    return eksternResultatService
        .getRapportPrKrav(rapportId, loeysingId)
        .fold(
            onSuccess = { resultatKrav -> ResponseEntity.ok(resultatKrav) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @GetMapping("rapport/{rapportId}/loeysing/{loeysingId}/testregel/{testregelId}")
  fun getDetaljertResultat(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
      @PathVariable testregelId: Int,
  ): ResponseEntity<out Any> {

    return kotlin
        .runCatching {
          eksternResultatService.getResultatListKontrollAsEksterntResultat(
              rapportId, loeysingId, testregelId)
        }
        .fold(
            onSuccess = { results -> ResponseEntity.ok(results) },
            onFailure = { ErrorHandlingUtil.handleErrors(it) })
  }

  @PutMapping("publiser/kontroll/{kontrollId}")
  fun publiserResultat(@PathVariable kontrollId: Int): ResponseEntity<Boolean> {
    logger.debug("Publiserer rapport for kontroll id $kontrollId")

    return try {
      publiseringService.publiser(kontrollId)
      ResponseEntity.ok(true)
    } catch (e: NoSuchElementException) {
      ResponseEntity.notFound().build()
    } catch (e: Exception) {
      ResponseEntity.badRequest().build()
    }
  }

  @GetMapping("bilder/sti")
  fun getBilde(@RequestParam("bildesti") bildesti: String): ResponseEntity<InputStreamResource> {

    if (!bildeService.erBildePublisert(bildesti)) {
      return ResponseEntity.status(HttpStatusCode.valueOf(404)).build()
    }

    return bildeService.getBildeResponse(bildesti)
  }

  @GetMapping("ekporter/rapport/{rapportId}/loeysing/{loeysingId}", produces = ["text/csv"])
  fun getEksporterResultatLoeysingCsv(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
      response: HttpServletResponse,
  ) {
    val testresults = eksternResultatService.eksporterRapportForLoeysing(rapportId, loeysingId)

    val fileName = "export.csv"

    val writer = response.writer.buffered()

    writer.write(""""Suksesskriterium", "Testregel", "Side", "Element"""")
    writer.newLine()
    testresults.forEach {
      writer.write(
          "\"${it.suksesskriterium}\", \"${it.testregelNoekkel}\", \"${it.side}\",\"${it.elementOmtale?.pointer ?: it.elementOmtale?.description ?: ""}\"")
      writer.newLine()
    }
    writer.flush()
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${fileName}; charset=UTF-8")
    response.setHeader(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
  }

  @GetMapping("ekporter/rapport/{rapportId}/loeysing/{loeysingId}/xlsx")
  fun getEksporterResultatLoeysingExcel(
      @PathVariable rapportId: String,
      @PathVariable loeysingId: Int,
  ): ResponseEntity<ByteArray> {

    val testresults = eksternResultatService.eksporterRapportForLoeysing(rapportId, loeysingId)

    val outputStream = ByteArrayOutputStream()

    val workbook = SXSSFWorkbook()
    val workSheet = workbook.createSheet()
    writeHeaders(workSheet)

    testresults.forEachIndexed { index, testresult ->
      writeDataRow(workSheet, testresult, index + 1)
    }

    val xmlProps: POIXMLProperties = workbook.xssfWorkbook.properties
    val coreProps = xmlProps.coreProperties
    coreProps.creator = "UU-tilsynet"

    workbook.write(outputStream)
    workbook.close()

    val headers = HttpHeaders()
    headers.contentType =
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    headers.setContentDispositionFormData("attachment", "resultat.xlsx")

    outputStream.flush()

    return ResponseEntity.ok().headers(headers).body(outputStream.toByteArray())
  }

  fun writeHeaders(workSheet: Sheet) {
    val headerRow = workSheet.createRow(0)
    headerRow.createCell(0).setCellValue("Suksesskriterium")
    headerRow.createCell(1).setCellValue("Testregel")
    headerRow.createCell(2).setCellValue("Side")
    headerRow.createCell(3).setCellValue("Element")
    headerRow.createCell(4).setCellValue("Utfall")
  }

  fun writeDataRow(workSheet: Sheet, testresultat: TestresultatDetaljert, rowNr: Int) {
    val dataRow = workSheet.createRow(rowNr)
    dataRow.createCell(0).setCellValue(testresultat.suksesskriterium.joinToString(", "))
    dataRow.createCell(1).setCellValue(testresultat.testregelNoekkel)
    dataRow.createCell(2).setCellValue(testresultat.side.toString())
    dataRow.createCell(3).setCellValue(getElement(testresultat))
    dataRow.createCell(4).setCellValue(testresultat.elementUtfall)
  }

  private fun getElement(testresultat: TestresultatDetaljert) =
      testresultat.elementOmtale?.pointer ?: testresultat.elementOmtale?.description ?: ""
}
