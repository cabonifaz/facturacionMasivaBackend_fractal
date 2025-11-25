package org.app.facturacion.application.services;

import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.app.facturacion.domain.exceptions.SystemException;
import org.app.facturacion.domain.models.InvoiceRow;
import org.app.facturacion.infrastructure.mappers.SheetRowMapper;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelReaderService {

  private Logger logger = LoggerFactory.getLogger(ExcelReaderService.class);

  public List<InvoiceRow> readInvoiceSheet(@NonNull MultipartFile file) {

    try (InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      SheetRowMapper mapper = new SheetRowMapper();

      return mapper.mapRows(sheet);
    } catch (Exception e) {
      this.logger.error("Error reading Excel: {}", e);
      throw new SystemException("Error reading Excel file: " + e, e);
    }
  }

}
