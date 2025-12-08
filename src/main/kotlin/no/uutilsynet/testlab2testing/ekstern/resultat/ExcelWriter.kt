package no.uutilsynet.testlab2testing.ekstern.resultat

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneId
import no.uutilsynet.testlab2testing.ekstern.resultat.model.TestEkstern
import no.uutilsynet.testlab2testing.testresultat.TestresultatDetaljert
import org.apache.poi.ooxml.POIXMLProperties
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExcelWriter {

    val logger = LoggerFactory.getLogger(ExcelWriter::class.java)

  fun writeResultsToSpreadsheet(
      testresults: List<TestresultatDetaljert>,
      kontrollInfo: TestEkstern,
  ): ByteArrayOutputStream {
    val outputStream = ByteArrayOutputStream()

    val workbook = SXSSFWorkbook(100000)
      val style = createHeaderStyle(workbook)
    val workSheet = workbook.createSheet()
    writeHeaders(workSheet, style)

    testresults.forEachIndexed { index, testresult ->
      writeDataRow(workSheet, testresult, kontrollInfo, index + 1)
    }
      autoSizeColumns(workSheet)

      logger.debug("Write etter streaming workbook with ${testresults.size} testresults")

      logger.debug("Write etter streaming workbook with ${testresults.size} testresults")

    val xmlProps: POIXMLProperties = workbook.xssfWorkbook.properties
    val coreProps = xmlProps.coreProperties
    coreProps.creator = "UU-tilsynet"

    workbook.write(outputStream)
    workbook.close()

    return outputStream
  }

  fun writeHeaders(workSheet: Sheet, style: CellStyle) {
    (workSheet as SXSSFSheet).trackAllColumnsForAutoSizing()

    val headerRow = workSheet.createRow(0)
    writeHeaderCell(workSheet, headerRow, style, 0, "Element")
    writeHeaderCell(workSheet, headerRow, style, 1, "Utfall")
    writeHeaderCell(workSheet, headerRow, style, 2, "Suksesskriterium")
    writeHeaderCell(workSheet, headerRow, style, 3, "Testregel")
    writeHeaderCell(workSheet, headerRow, style, 4, "Side")
    writeHeaderCell(workSheet, headerRow, style, 5, "Verksemd")
    writeHeaderCell(workSheet, headerRow, style, 6, "IKT-Løysing")
    writeHeaderCell(workSheet, headerRow, style, 7, "Dato for kontroll")
    writeHeaderCell(workSheet, headerRow, style, 8, "Type kontroll")
  }

  private fun createHeaderStyle(workbook: SXSSFWorkbook): CellStyle {
    val font: Font = workbook.createFont()
    font.bold = true // Make text bold

    val style: CellStyle = workbook.createCellStyle()
    style.shrinkToFit
    style.setFont(font)
    style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
    style.fillPattern = FillPatternType.SOLID_FOREGROUND
    return style
  }

  private fun writeHeaderCell(
      workSheet: Sheet,
      headerRow: Row,
      style: CellStyle,
      columnIndex: Int,
      cellValue: String
  ) {
    val cell = headerRow.createCell(columnIndex)
    cell.setCellValue(cellValue)
    cell.cellStyle = style
    workSheet.autoSizeColumn(columnIndex)
  }

  fun writeDataRow(
      workSheet: SXSSFSheet,
      testresultat: TestresultatDetaljert,
      kontrollInfo: TestEkstern,
      rowNr: Int,
  ) {
      val dataRow = workSheet.createRow(rowNr)
      dataRow.createCell(0).setCellValue(getElement(testresultat))
      dataRow.createCell(1).setCellValue(testresultat.elementUtfall)
      dataRow.createCell(2).setCellValue(testresultat.suksesskriterium.joinToString(", "))
      dataRow.createCell(3).setCellValue(testresultat.testregelNoekkel)
      dataRow.createCell(4).setCellValue(testresultat.side.toString())
      dataRow.createCell(5).setCellValue(kontrollInfo.organisasjonsnamn)
      dataRow.createCell(6).setCellValue(kontrollInfo.loeysingNamn)
      dataRow
          .createCell(7)
          .setCellValue(LocalDate.ofInstant(kontrollInfo.utfoert, ZoneId.systemDefault()).toString())
      dataRow.createCell(8).setCellValue(kontrollInfo.kontrollType.name)
      if (rowNr % 5000 == 0) {
          logger.debug("Row $rowNr")
        }
  }

  private fun autoSizeColumns(workSheet: Sheet) {
    workSheet.autoSizeColumn(0)
    workSheet.autoSizeColumn(1)
    workSheet.autoSizeColumn(2)
    workSheet.autoSizeColumn(3)
    workSheet.autoSizeColumn(4)
    workSheet.autoSizeColumn(5)
    workSheet.autoSizeColumn(6)
    workSheet.autoSizeColumn(7)
    workSheet.autoSizeColumn(8)
  }

  private fun getElement(testresultat: TestresultatDetaljert) =
      testresultat.elementOmtale?.pointer ?: testresultat.elementOmtale?.description ?: ""
}
