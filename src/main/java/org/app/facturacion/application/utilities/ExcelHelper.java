package org.app.facturacion.application.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.app.facturacion.domain.models.InvoicesTableReport;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExcelHelper {

  /**
   * Genera un reporte de facturas en formato Excel (byte array).
   * 
   * @return byte[] Los bytes del archivo Excel
   * @throws IOException
   */
  public static byte[] generateInvoiceReport(List<InvoicesTableReport> invoiceData) throws IOException {
    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("Reporte Facturas");

    // ── Header style
    var headerStyle = (XSSFCellStyle) workbook.createCellStyle();
    byte[] rgb = new byte[] { (byte) 0, (byte) 176, (byte) 240 };
    headerStyle.setFillForegroundColor(new XSSFColor(rgb, new DefaultIndexedColorMap()));
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerFont.setColor(IndexedColors.WHITE.getIndex());
    headerStyle.setFont(headerFont);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    applyPadding(headerStyle);

    // ── Numeric style
    var numericStyle = (XSSFCellStyle) workbook.createCellStyle();
    DataFormat dataFormat = workbook.createDataFormat();
    numericStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));
    applyPadding(numericStyle);

    // ── Date style
    var dateStyle = (XSSFCellStyle) workbook.createCellStyle();
    dateStyle.setDataFormat(dataFormat.getFormat("dd-MM-yyyy"));
    applyPadding(dateStyle);

    // ── Default text style
    var textStyle = (XSSFCellStyle) workbook.createCellStyle();
    applyPadding(textStyle);

    // ── Headers map
    Map<String, Integer> headersMap = new LinkedHashMap<>();
    headersMap.put("Cliente", 0);
    headersMap.put("Analítica", 1);
    headersMap.put("OC/OS", 2);
    headersMap.put("NI/CS", 3);
    headersMap.put("Colaborador", 4);
    headersMap.put("Inicio", 5);
    headersMap.put("Fin", 6);
    headersMap.put("Concepto", 7);
    headersMap.put("Moneda", 8);
    headersMap.put("Monto", 9);
    headersMap.put("IGV", 10);
    headersMap.put("Total", 11);
    headersMap.put("Contacto", 12);
    headersMap.put("N. Factura", 13);

    var headerRow = sheet.createRow(0);
    headersMap.forEach((name, col) -> {
      var cell = headerRow.createCell(col);
      cell.setCellValue(name);
      cell.setCellStyle(headerStyle);
    });

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    int rowNum = 1;
    for (var data : invoiceData) {
      var row = sheet.createRow(rowNum++);
      row.setHeightInPoints(25f);

      var incomingNumberStr = data.getIncomingNumber().toString();

      createTextCell(row, headersMap.get("Cliente"), data.getClientName(), textStyle);
      createTextCell(row, headersMap.get("Analítica"), data.getAnalytic(), textStyle);
      createTextCell(row, headersMap.get("OC/OS"), data.getObservation(), textStyle);
      createTextCell(row, headersMap.get("NI/CS"), incomingNumberStr, textStyle);
      createTextCell(row, headersMap.get("Colaborador"), data.getCollaborator(), textStyle);

      // Date cells
      createDateCell(row, headersMap.get("Inicio"), data.getStartDate(), formatter, dateStyle, workbook);
      createDateCell(row, headersMap.get("Fin"), data.getEndDate(), formatter, dateStyle, workbook);

      createTextCell(row, headersMap.get("Concepto"), data.getConcept(), textStyle);
      createTextCell(row, headersMap.get("Moneda"), data.getCurrencyName(), textStyle);

      // Numeric cells
      createNumericCell(row, headersMap.get("Monto"), data.getPricePerUnit().doubleValue(), numericStyle);
      createNumericCell(row, headersMap.get("IGV"), data.getIgv().doubleValue(), numericStyle);
      createNumericCell(row, headersMap.get("Total"), data.getTotalToPay().doubleValue(), numericStyle);

      createTextCell(row, headersMap.get("Contacto"), data.getContact(), textStyle);
      createTextCell(row, headersMap.get("N. Factura"), data.getInvoiceNumber(), textStyle);
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      headersMap.forEach((name, col) -> {
        sheet.autoSizeColumn(col);
        int minWidth = 20 * 256; // Min 20 characters
        if (sheet.getColumnWidth(col) < minWidth)
          sheet.setColumnWidth(col, minWidth);
      });
      workbook.write(outputStream);
      return outputStream.toByteArray();
    } finally {
      workbook.close();
    }
  }

  private static void applyPadding(XSSFCellStyle style) {
    style.setWrapText(true);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
  }

  private static void createTextCell(Row row, int col, String value, XSSFCellStyle style) {
    var cell = row.createCell(col);
    cell.setCellValue(value != null ? value : "");
    cell.setCellStyle(style);
  }

  private static void createNumericCell(Row row, int col, double value, XSSFCellStyle style) {
    var cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private static void createDateCell(Row row, int col, String dateStr,
      DateTimeFormatter formatter, XSSFCellStyle style, Workbook workbook) {
    var cell = row.createCell(col);
    try {
      LocalDate date = LocalDate.parse(dateStr, formatter);
      cell.setCellValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
      cell.setCellStyle(style);
    } catch (Exception e) {
      cell.setCellValue(dateStr != null ? dateStr : "");
    }
  }

}
