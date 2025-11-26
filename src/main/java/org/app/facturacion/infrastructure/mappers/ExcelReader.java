package org.app.facturacion.infrastructure.mappers;

import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.app.facturacion.domain.exceptions.SystemAPIException;
import org.app.facturacion.domain.models.InvoiceRow;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

public class ExcelReader {

  private Logger logger = LoggerFactory.getLogger(ExcelReader.class);

  public List<InvoiceRow> readInvoiceSheet(@NonNull MultipartFile file) throws SystemAPIException {

    this.logger.debug("Processing file: {}", file.getOriginalFilename());

    try (InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      SheetRowMapper mapper = new SheetRowMapper();

      return mapper.mapRows(sheet);

    } catch (Exception e) {
      this.logger.error("Error reading Excel: {}", e);
      throw new SystemAPIException("Error reading Excel file: " + e, e);
    }
  }

}
