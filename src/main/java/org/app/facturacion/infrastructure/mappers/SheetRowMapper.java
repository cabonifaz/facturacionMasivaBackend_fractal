package org.app.facturacion.infrastructure.mappers;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.app.facturacion.domain.models.InvoiceRow;

public class SheetRowMapper {

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
      if (row == null)
        continue;

      InvoiceRow data = new InvoiceRow();

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
    }
    return rows;
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

}