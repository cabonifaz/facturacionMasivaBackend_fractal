package org.app.facturacion.application.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
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
  @SuppressWarnings("null")
  public static byte[] generateInvoiceReport(List<InvoicesTableReport> invoiceData) throws IOException {
    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("Reporte Facturas");
    var headerStyle = (XSSFCellStyle) workbook.createCellStyle();

    byte[] rgb = new byte[] { (byte) 0, (byte) 176, (byte) 240 };
    var backColor = new XSSFColor(rgb, new DefaultIndexedColorMap());
    headerStyle.setFillForegroundColor(backColor);
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerFont.setColor(IndexedColors.WHITE.getIndex());
    headerStyle.setFont(headerFont);

    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

    // Numeric cell style
    var numericStyle = (XSSFCellStyle) workbook.createCellStyle();
    DataFormat dataFormat = workbook.createDataFormat();
    numericStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

    // Column name -> position
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

    // Create headers
    var headerRow = sheet.createRow(0);
    headersMap.forEach((name, col) -> {
      var cell = headerRow.createCell(col);
      cell.setCellValue(name);
      cell.setCellStyle(headerStyle);
    });

    int rowNum = 1;
    for (var data : invoiceData) {
      var row = sheet.createRow(rowNum++);
      row.createCell(headersMap.get("Cliente")).setCellValue(data.getClientName());
      row.createCell(headersMap.get("Analítica")).setCellValue(data.getAnalytic());
      row.createCell(headersMap.get("OC/OS")).setCellValue(data.getObservation());
      row.createCell(headersMap.get("NI/CS")).setCellValue(data.getIncomingNumber());
      row.createCell(headersMap.get("Colaborador")).setCellValue(data.getCollaborator());
      row.createCell(headersMap.get("Inicio")).setCellValue(data.getStartDate());
      row.createCell(headersMap.get("Fin")).setCellValue(data.getEndDate());
      row.createCell(headersMap.get("Concepto")).setCellValue(data.getConcept());
      row.createCell(headersMap.get("Moneda")).setCellValue(data.getCurrencyName());

      // Numeric cells
      var montoCell = row.createCell(headersMap.get("Monto"));
      montoCell.setCellValue(data.getPricePerUnit().doubleValue());
      montoCell.setCellStyle(numericStyle);

      var igvCell = row.createCell(headersMap.get("IGV"));
      igvCell.setCellValue(data.getIgv().doubleValue());
      igvCell.setCellStyle(numericStyle);

      var totalCell = row.createCell(headersMap.get("Total"));
      totalCell.setCellValue(data.getTotalToPay().doubleValue());
      totalCell.setCellStyle(numericStyle);

      row.createCell(headersMap.get("Contacto")).setCellValue(data.getContact());
      row.createCell(headersMap.get("N. Factura")).setCellValue(data.getInvoiceNumber());
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      headersMap.values().forEach(sheet::autoSizeColumn);
      workbook.write(outputStream);
      return outputStream.toByteArray();
    } finally {
      workbook.close();
    }
  }

}
