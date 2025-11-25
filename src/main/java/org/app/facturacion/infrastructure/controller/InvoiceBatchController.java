package org.app.facturacion.infrastructure.controller;

import java.util.List;

import org.app.facturacion.application.services.ExcelReaderService;
import org.app.facturacion.domain.models.BaseAPIResponse;
import org.app.facturacion.domain.models.InvoiceRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController()
@RequestMapping("/invoices-batch")
@Validated
public class InvoiceBatchController {

  private Logger logger = LoggerFactory.getLogger(InvoiceBatchController.class);

  private final ExcelReaderService excelReaderService;

  public InvoiceBatchController(ExcelReaderService service) {
    this.excelReaderService = service;
  }

  @PostMapping("/upload-batch")
  public BaseAPIResponse<List<InvoiceRow>> processSheetFile(
      @RequestParam("file") MultipartFile file) {
    logger.info("File param name: {}", file.getName());
    logger.info("Original filename: {}", file.getOriginalFilename());
    logger.info("Size: {}", file.getSize());
    logger.info("ContentType: {}", file.getContentType());
    ;
    List<InvoiceRow> rows = this.excelReaderService.readInvoiceSheet(file);

    return BaseAPIResponse.success("Datos procesados correctamente", rows);
  }
}
