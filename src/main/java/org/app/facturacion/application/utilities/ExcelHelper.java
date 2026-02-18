package org.app.facturacion.application.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
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
  public static byte[] generateInvoiceReport(List<InvoicesTableReport> invoiceData) throws IOException {

    Workbook workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("Reporte Facturas");

    var headerStyle = (XSSFCellStyle) workbook.createCellStyle();

    // Background color
    byte[] rgb = new byte[] { (byte) 0, (byte) 176, (byte) 240 };
    var backColor = new XSSFColor(rgb, new DefaultIndexedColorMap());
    headerStyle.setFillForegroundColor(backColor);
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

    // Font
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerFont.setColor(IndexedColors.WHITE.getIndex());
    headerStyle.setFont(headerFont);

    // Borders
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

    Integer rowNum = 1;
    String[] headers = {
        "Cliente",
        "Analítica",
        "OC/OS",
        "NI/CS",
        "Colaborador",
        "Inicio",
        "Fin",
        "Concepto",
        "Moneda",
        "Monto",
        "IGV",
        "Total",
        "Contacto",
        "N. Factura"
    };

    var headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      var cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }

    for (var data : invoiceData) {
      var row = sheet.createRow(rowNum++);

      row.createCell(0).setCellValue("BANBIF");
      row.createCell(1).setCellValue("OUT.MTT");
      row.createCell(2).setCellValue(data.getOrderNumber());
      row.createCell(3).setCellValue(data.getIncommingNumber());

      row.createCell(4).setCellValue(data.getCollaborator());
      row.createCell(5).setCellValue(data.getStartDate());
      row.createCell(6).setCellValue(data.getEndDate());
      row.createCell(7).setCellValue(data.getConcept());

      row.createCell(8).setCellValue(data.getCurrencyName());
      row.createCell(9).setCellValue(data.getPricePerUnit().toPlainString());
      row.createCell(10).setCellValue(data.getIgv().toPlainString());

      var subTotal = data.getPricePerUnit().add(data.getIgv());

      row.createCell(11).setCellValue(subTotal.toPlainString());

      row.createCell(12).setCellValue(data.getContact());
      row.createCell(13).setCellValue(data.getInvoiceNumber());
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      for (int i = 0; i < headers.length; i++)
        sheet.autoSizeColumn(i);

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } finally {
      workbook.close();
    }

  }

}
