package no.uutilsynet.testlab2testing.ekstern.resultat

import no.uutilsynet.testlab2testing.dto.TestresultatDetaljert
import org.apache.poi.ooxml.POIXMLProperties
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream


@Service
class ExcelWriter {

    fun writeResultsToSpreadsheet(
        testresults: List<TestresultatDetaljert>,
        kontrollInfo: TestEkstern,
    ): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()

        val workbook = SXSSFWorkbook()
        val style = createHeaderStyle(workbook)
        val workSheet = workbook.createSheet()


        writeMetadata(workSheet, kontrollInfo)

        writeResults(workSheet, style, testresults)

        val xmlProps: POIXMLProperties = workbook.xssfWorkbook.properties
        val coreProps = xmlProps.coreProperties
        coreProps.creator = "UU-tilsynet"

        workbook.write(outputStream)
        workbook.close()

        return outputStream
    }

    private fun writeMetadata(workSheet: Sheet, kontrollInfo: TestEkstern) {
        writeMetadataRow(workSheet, "Verksemd", kontrollInfo.organisasjonsnamn, 0)
        writeMetadataRow(workSheet, "IKT-Løysing", kontrollInfo.loeysingNamn, 1)
        writeMetadataRow(workSheet, "Dato for kontroll", kontrollInfo.utfoert.toString(), 2)
        writeMetadataRow(workSheet, "Type kontroll", kontrollInfo.kontrollType.toString(), 3)
    }

    private fun writeMetadataRow(
        workSheet: Sheet,
        label: String,
        value: String,
        rowIndex: Int,
    ) {
        val style =  createMetadataHeaderStyle((workSheet as SXSSFSheet).workbook as SXSSFWorkbook)
        val virksomhetRow = workSheet.createRow(rowIndex)
        writeHeaderCell(workSheet, virksomhetRow, style, 0, label)
        virksomhetRow.createCell(1).setCellValue(value)
    }


    private fun writeResults(
        workSheet: SXSSFSheet,
        style: CellStyle,
        testresults: List<TestresultatDetaljert>,
    ) {
        writeHeaders(workSheet, style)

        testresults.forEachIndexed { index, testresult ->
            writeDataRow(workSheet, testresult, index + 7)
        }
    }

    fun writeHeaders(workSheet: Sheet, style: CellStyle) {
        (workSheet as SXSSFSheet).trackAllColumnsForAutoSizing()

        val headerRow = workSheet.createRow(6)
        writeHeaderCell(workSheet, headerRow, style, 0, "Side",true)
        writeHeaderCell(workSheet, headerRow, style, 1, "Suksesskriterium", true)
        writeHeaderCell(workSheet, headerRow, style, 2, "Testregel", true)
        writeHeaderCell(workSheet, headerRow, style, 3, "Utfall", true)
        writeHeaderCell(workSheet,headerRow, style,4, "Element", true)

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

    private fun createMetadataHeaderStyle(workbook: SXSSFWorkbook): CellStyle {
        val font: Font = workbook.createFont()
        font.bold = true // Make text bold

        val style: CellStyle = workbook.createCellStyle()
        style.shrinkToFit
        style.setFont(font)
        return style
    }

    private fun writeHeaderCell(workSheet: Sheet, headerRow: Row, style: CellStyle, columnIndex: Int, cellValue: String,track:Boolean = false) {
        val cell = headerRow.createCell(columnIndex)
        cell.setCellValue(cellValue)
        cell.cellStyle = style
        if (track)  workSheet.autoSizeColumn(columnIndex)

    }

    fun writeDataRow(
        workSheet: Sheet,
        testresultat: TestresultatDetaljert,
        rowNr: Int,
    ) {
        val dataRow = workSheet.createRow(rowNr)
        dataRow.createCell(0).setCellValue(testresultat.side.toString())
        dataRow.createCell(1).setCellValue(testresultat.suksesskriterium[0])
        dataRow.createCell(2).setCellValue(testresultat.testregelNoekkel)
        dataRow.createCell(3).setCellValue(testresultat.elementUtfall)
        dataRow.createCell(4).setCellValue(getElement(testresultat))
        autoSizeColumns(workSheet)


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