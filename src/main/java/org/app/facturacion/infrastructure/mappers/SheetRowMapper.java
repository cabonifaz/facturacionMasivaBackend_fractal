package org.app.facturacion.infrastructure.mappers;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.app.facturacion.domain.models.InvoiceRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SheetRowMapper {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Integer clientNameCol = 0;
  private final Integer analyticCol = 1;
  private final Integer ocOsCol = 2;
  private final Integer niCsCol = 3;
  private final Integer collaboratorCol = 4;
  private final Integer startDateCol = 5;
  private final Integer endDateCol = 6;
  private final Integer conceptCol = 7;
  private final Integer currencyCol = 8;
  private final Integer amountCol = 9;
  private final Integer igvCol = 10;
  private final Integer totalAmountCol = 11;
  private final Integer contactCol = 12;

  public List<InvoiceRow> mapRows(Sheet sheet) {
    List<InvoiceRow> rows = new ArrayList<>();

    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);

      // 1. Validación básica de fila nula
      if (row == null)
        continue;

      // 2. Validación de Fila Vacía
      if (isCellEmpty(row.getCell(this.clientNameCol)))
        continue;

      InvoiceRow data = new InvoiceRow();

      try {
        data.setClientName(getCellString(row.getCell(this.clientNameCol)));
        data.setAnalytic(getCellString(row.getCell(this.analyticCol)));
        data.setOcOs(getCellString(row.getCell(this.ocOsCol)));
        data.setNiCs(getCellInteger(row.getCell(this.niCsCol)));
        data.setCollaborator(getCellString(row.getCell(this.collaboratorCol)));
        data.setStartDate(getCellString(row.getCell(this.startDateCol)));
        data.setEndDate(getCellString(row.getCell(this.endDateCol)));
        data.setConcept(getCellString(row.getCell(this.conceptCol)));
        data.setCurrency(getCellString(row.getCell(this.currencyCol)));
        data.setAmount(getCellDouble(row.getCell(this.amountCol)));
        data.setIgv(getCellDouble(row.getCell(this.igvCol)));
        data.setTotalAmount(getCellDouble(row.getCell(this.totalAmountCol)));
        data.setContact(getCellString(row.getCell(this.contactCol)));

        rows.add(data);
        this.logger.info("Row: {} successfuly processed", rowIndex);
      } catch (Exception e) {
        this.logger.error("Error procesando fila {}: {}", rowIndex, e.getMessage());
      }
    }
    return rows;
  }

  private boolean isCellEmpty(Cell cell) {
    return cell == null || cell.getCellType() == CellType.BLANK ||
        (cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty());
  }

  private String getCellString(Cell cell) {
    if (cell == null)
      return null;
    return cell.getCellType() == CellType.STRING ? cell.getStringCellValue() : cell.toString();
  }

  private Double getCellDouble(Cell cell) {
    if (cell == null)
      return null;
    return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : Double.valueOf(cell.toString());
  }

  private Integer getCellInteger(Cell cell) {
    if (cell == null)
      return null;
    Double val = cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : Double.valueOf(cell.toString());
    return val.intValue();
  }

  public List<ActivityReportRow> mapActivityReport(Sheet sheet) {

    var rows = new ArrayList<ActivityReportRow>();

    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);

      if (row == null)
        continue;

      // 2. Validación de Fila Vacía
      if (isCellEmpty(row.getCell(this.clientNameCol)))
        continue;

      var dataBuilder = ActivityReportRow.builder();

      try {
        dataBuilder.provider(getCellString(row.getCell(0)));
        dataBuilder.pucharseOrder(getCellString(row.getCell(1)));
        dataBuilder.ocOs(getCellString(row.getCell(2)));
        dataBuilder.initiativeId(getCellString(row.getCell(3)));
        dataBuilder.resourceName(getCellString(row.getCell(4)));
        dataBuilder.resourceProfile(getCellString(row.getCell(5)));
        dataBuilder.servicePeriod(getCellString(row.getCell(6)));
        dataBuilder.activities(getCellString(row.getCell(7)));
        dataBuilder.activitiesDetails(getCellString(row.getCell(8)));
        dataBuilder.manager(getCellString(row.getCell(9)));
        dataBuilder.managment(getCellString(row.getCell(10)));
        dataBuilder.feedback(getCellString(row.getCell(11)));
        dataBuilder.incommingNote(getCellInteger(row.getCell(12)));
        dataBuilder.invoiceSerial(getCellString(row.getCell(13)));

        var data = dataBuilder.build();
        rows.add(data);
        this.logger.debug("Row: {} successfuly processed", rowIndex);
      } catch (Exception e) {
        this.logger.error("Error procesando fila {}: {}", rowIndex, e.getMessage());
      }
    }
    return rows;
  }

}